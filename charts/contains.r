
source("../common.r")

data = jmhCSV("contains.csv")

data = data[,grep("^(Benchmark|Score|Error|size)$", colnames(data))] # keep only these columns

data$size = sub("(.*)", "\\1", data$size) # convert size numbers to strings for better colors

g1 = jmhBarChart(subset(data, size == "100"), "Benchmark", "size", "", "", "Contains 100")
g2 = jmhBarChart(subset(data, size == "256"), "Benchmark", "size", "", "", "Contains 256")
g3 = jmhBarChart(subset(data, size == "1000"), "Benchmark", "size", "", "", "Contains 1,000")
g4 = jmhBarChart(subset(data, size == "10000"), "Benchmark", "size", "", "", "Contains 10,000")
g5 = jmhBarChart(subset(data, size == "100000"), "Benchmark", "size", "", "", "Contains 100,000")
g6 = jmhBarChart(subset(data, size == "1000000"), "Benchmark", "size", "", "", "Contains 1,000,000")

if (!rstudio) png("contains.png", 1536, 1024)
grid.arrange(g1, g2, g3, g4, g5, g6)
