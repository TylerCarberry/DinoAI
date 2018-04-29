# Dino AI

A genetic algorithm that learns to solve the [Chrome Dinosaur Game](chrome://dino).

Developed by [Tyler Carberry](https://github.com/tylercarberry) and [Cole Robertson](https://github.com/colerobertson) for our Graduate AI class.

![Demo](https://user-images.githubusercontent.com/6628497/39097331-e7dca934-4628-11e8-85bb-821dd2dbe0f4.gif)


We modified the game to take input from query parameters. These parameters are created from the genetic algorithm and get refined over time.

• Survival: Every round the top 10% of the population gets passed down directly  
• Crossover: The top 50% of the population breed and pass on a combination of their genes  
• Mutations: There is a 30% chance that an individual has a mutation which slightly modifies one of its genes  

## Usage
Download Google Chrome if not already installed  
Install chromedriver by running `brew install chromedriver`  
Run `Driver.java`
