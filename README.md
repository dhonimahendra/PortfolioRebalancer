# Portfolio Rebalancer

## Problem Statement
The **Portfolio Rebalancer** is a Java application that helps portfolio managers rebalance their portfolios according to a given model.  
It calculates the target quantities of each security to align the current portfolio with the desired allocation percentages.

---

## Inputs
1. **Portfolio CSV (`portfolio.csv`)**  
   Format:
Security,Price,Quantity
USD,1,100000
IBM,120.5,500
MSFT,30.8,200
AAPL,2908.65,400
META,1700.5,0
TSLA,3200.7,0

2. **Model CSV (`model.csv`)**  
Format:
Security,Percentage
USD,20
META,25
TSLA,35
IBM,5
AAPL,15

---

## Output
The program generates a CSV (`output.csv`) showing the **target portfolio**:

Security,Price,Current Qty.,Target Qty.,Target Percent
USD,1,100000,265257.65,19.95
IBM,120.5,500,552,5
MSFT,30.8,200,0,0
AAPL,2908.65,400,69,15.09
META,1700.5,0,196,25.06
TSLA,3200.7,0,145,34.9


---

## How to Run Locally

1. Open terminal in the project root.  
2. Compile the code:
```bash
javac -d bin src/com/rebalancer/*.java

java -cp bin com.rebalancer.Rebalancer data/portfolio.csv data/model.csv data/output.csv
