package com.github.mizosoft.timetablegenerator;

public class Main {
  public static void main(String[] args) {
    var solver = new Solver(Samples.readInstance("NE-CESVP-2011-M-D.xml"));
    solver.geneticAlgorithm();
  }
}
