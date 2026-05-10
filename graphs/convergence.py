import matplotlib.pyplot as plt

# Fitness values across iterations
fitness_values = [0.248800 ,0.248800 ,0.248800 ,0.248800 ,0.248800 ,0.248800 ,0.248800 ,0.248800 ,0.248800 ,0.248800 ,0.248800 ,0.248800 ,0.248800 ,0.248800 ,0.248800 ,0.248800 ,0.248800 ,0.248800 ,0.248800 ,0.248800 ,0.248800 ,0.248800 ,0.248800 ,0.248800 ,0.248800 ,0.248800 ,0.248800 ,0.248800 ,0.248800 ,0.248800]

# Iteration numbers
iterations = list(range(1, len(fitness_values) + 1))

# Create plot
plt.figure(figsize=(8, 5))
plt.plot(iterations, fitness_values, marker='o')
# plt.plot(iterations, fitness_values, linewidth=2)

# Labels and title
plt.xlabel("Iterations")
plt.ylabel("Fitness")
plt.title("Convergence Plot: Fitness vs Iterations")

# Grid
plt.grid(True)

# Show plot
plt.show()