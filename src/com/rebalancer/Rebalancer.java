package com.rebalancer;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Rebalancer {
    private static final String USD = "USD";

    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("Usage: java Rebalancer <portfolioCsvPath> <modelCsvPath> <outputCsvPath>");
            System.exit(1);
        }
        File portfolioFile = new File(args[0]);
        File modelFile = new File(args[1]);
        File outputFile = new File(args[2]);

        try {
            Map<String, Position> portfolio = readPortfolio(portfolioFile);
            Map<String, Allocation> model = readModel(modelFile);

            List<TargetLine> targets = rebalance(portfolio, model);
            writeOutput(outputFile, targets, portfolio);

            System.out.println("Rebalance completed. Output written to: " + outputFile.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(2);
        }
    }

    // ----------------- Core Rebalancing Logic -----------------

    private static List<TargetLine> rebalance(Map<String, Position> portfolio,
                                              Map<String, Allocation> model) {

        Set<String> universe = new TreeSet<>();
        universe.addAll(portfolio.keySet());
        universe.addAll(model.keySet());

        BigDecimal totalValue = BigDecimal.ZERO;
        for (Position p : portfolio.values()) {
            totalValue = totalValue.add(p.getMarketValue());
        }

        Map<String, Long> targetQtys = new HashMap<>();
        Map<String, BigDecimal> targetValues = new HashMap<>();
        BigDecimal sumNonUsdTargetValues = BigDecimal.ZERO;

        for (String sec : universe) {
            if (USD.equalsIgnoreCase(sec)) continue;
            Position pos = portfolio.get(sec);
            Allocation alloc = model.getOrDefault(sec, new Allocation(sec, BigDecimal.ZERO));

            BigDecimal modelPct = alloc.getPercent();
            BigDecimal idealTargetValue = totalValue.multiply(modelPct).divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
            BigDecimal idealTargetQty = idealTargetValue.divide(pos.getPrice(), 10, RoundingMode.HALF_UP);

            BigDecimal diff = idealTargetQty.subtract(BigDecimal.valueOf(pos.getQuantity()));
            long tradeQty = diff.setScale(0, RoundingMode.HALF_UP).longValue();
            long tgtQty = Math.max(0, pos.getQuantity() + tradeQty);

            BigDecimal tgtVal = pos.getPrice().multiply(BigDecimal.valueOf(tgtQty));
            targetQtys.put(sec, tgtQty);
            targetValues.put(sec, tgtVal);
            sumNonUsdTargetValues = sumNonUsdTargetValues.add(tgtVal);
        }

        Position usdPos = portfolio.get(USD);
        BigDecimal usdTargetValue = totalValue.subtract(sumNonUsdTargetValues);
        BigDecimal usdTargetQty = usdTargetValue.divide(usdPos.getPrice(), 10, RoundingMode.HALF_UP);

        List<TargetLine> results = new ArrayList<>();
        for (String sec : order(universe, portfolio)) {
            Position pos = portfolio.get(sec);
            BigDecimal tgtVal;
            long tgtQty;

            if (USD.equalsIgnoreCase(sec)) {
                tgtVal = usdTargetValue;
                tgtQty = usdTargetQty.longValue(); // store rounded qty
            } else {
                tgtVal = targetValues.getOrDefault(sec, BigDecimal.ZERO);
                tgtQty = targetQtys.getOrDefault(sec, 0L);
            }

            BigDecimal pct = (totalValue.signum() == 0) ? BigDecimal.ZERO
                    : tgtVal.multiply(BigDecimal.valueOf(100)).divide(totalValue, 4, RoundingMode.HALF_UP);

            results.add(new TargetLine(sec, pos.getPrice(), pos.getQuantity(), tgtQty, pct));
        }
        return results;
    }

    private static List<String> order(Set<String> universe, Map<String, Position> portfolio) {
        List<String> order = new ArrayList<>();
        if (universe.contains(USD)) order.add(USD);
        for (String sec : portfolio.keySet()) {
            if (!USD.equalsIgnoreCase(sec) && universe.contains(sec)) {
                order.add(sec);
            }
        }
        return order;
    }

    //===== File I/O =====

    private static Map<String, Position> readPortfolio(File portfolioFile) throws IOException {
        Map<String, Position> portfolio = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(portfolioFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("Security")) {
                    continue; // skip header or empty line
                }
                String[] parts = line.split(",");
                if (parts.length < 3) {
                    System.err.println("Skipping malformed portfolio line: " + line);
                    continue;
                }
                String symbol = parts[0].trim();
                BigDecimal price = new BigDecimal(parts[1].trim());
                long qty = Long.parseLong(parts[2].trim());
                portfolio.put(symbol, new Position(symbol, price, qty));

            }
        }
        return portfolio;
    }


    private static Map<String, Allocation> readModel(File f) throws IOException {
        Map<String, Allocation> map = new LinkedHashMap<>();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))) {
            
            String header = br.readLine(); // skip header
            String line;
            BigDecimal sum = BigDecimal.ZERO;
            boolean hasUsd = false;

            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue; // ✅ skip blank lines
                }

                String[] parts = line.split(",", -1);
                if (parts.length < 2) {
                    System.err.println("⚠️ Skipping malformed model line: " + line);
                    continue; // ✅ skip bad lines safely
                }

                String sec = parts[0].trim();
                BigDecimal pct;
                try {
                    pct = new BigDecimal(parts[1].trim());
                } catch (NumberFormatException e) {
                    throw new IOException("Invalid percentage for security " + sec + ": " + parts[1], e);
                }

                map.put(sec, new Allocation(sec, pct));
                sum = sum.add(pct);
                if (USD.equalsIgnoreCase(sec)) hasUsd = true;
            }

            if (!hasUsd) throw new IOException("Model must include USD.");
            if (sum.compareTo(BigDecimal.valueOf(100)) != 0)
                throw new IOException("Model percentages must sum to 100, got: " + sum);
        }
        return map;
    }


    private static void writeOutput(File f, List<TargetLine> lines, Map<String, Position> portfolio) throws IOException {
        BigDecimal total = portfolio.values().stream().map(Position::getMarketValue).reduce(BigDecimal.ZERO, BigDecimal::add);
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8))) {
            pw.println("Security,Price,Current Qty.,Target Qty.,Target Percent");
            for (TargetLine tl : lines) {
                BigDecimal tgtValue = tl.getPrice().multiply(BigDecimal.valueOf(tl.getTargetQty()));
                BigDecimal pct = (total.signum() == 0) ? BigDecimal.ZERO
                        : tgtValue.multiply(BigDecimal.valueOf(100)).divide(total, 2, RoundingMode.HALF_UP);

                pw.printf(Locale.US, "%s,%s,%d,%d,%.2f%n",
                        tl.getSecurity(),
                        tl.getPrice().stripTrailingZeros().toPlainString(),
                        tl.getCurrentQty(),
                        tl.getTargetQty(),
                        pct);
            }
        }
    }
}
