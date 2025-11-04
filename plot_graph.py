import pandas as pd
import matplotlib.pyplot as plt

# Load the CSV file (exported from Java program)
# Make sure the Java output is saved as performance.csv with tabs or commas
# Example CSV format:
# Image,Threshold,Runtime(ms),Compression Ratio
data = pd.read_csv("performance.csv")  # Or use sep="\t" if tab-separated

# Plot Compression Ratio vs Threshold
plt.figure(figsize=(10, 5))
for img in data['Image'].unique():
    subset = data[data['Image'] == img]
    plt.plot(subset['Threshold'], subset['Compression Ratio'], marker='o', label=img)
plt.xlabel("Threshold")
plt.ylabel("Compression Ratio")
plt.title("QuadTree Compression Ratio vs Threshold")
plt.legend()
plt.grid(True)
plt.savefig("compression_ratio_vs_threshold.png")
plt.show()

# Plot Runtime vs Threshold
plt.figure(figsize=(10, 5))
for img in data['Image'].unique():
    subset = data[data['Image'] == img]
    plt.plot(subset['Threshold'], subset['Runtime(ms)'], marker='o', label=img)
plt.xlabel("Threshold")
plt.ylabel("Runtime (ms)")
plt.title("QuadTree Runtime vs Threshold")
plt.legend()
plt.grid(True)
plt.savefig("runtime_vs_threshold.png")
plt.show()