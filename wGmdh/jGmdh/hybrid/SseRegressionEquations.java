/*
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/*
 *    Copyright (C) 2010 Ivan Ivek
 */
package wGmdh.jGmdh.hybrid;

/**
 *
 * @author ivek
 */
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import wGmdh.jGmdh.exceptions.*;
import wGmdh.jGmdh.oldskul.LinearEqSystem;
import wGmdh.jGmdh.oldskul.Model;
import wGmdh.jGmdh.oldskul.Node;

public class SseRegressionEquations {

    /* The idea is to build an array of indices of polynomial variables similar
     * to this (example: 3 variables)
     * 
    // NIL,       x1          x2          x3          x1*x1        x1*x2        x1*x3        x2*x2        x2*x3        x3*x3      
    // x1         x1*x1      
    // x2         x1*x2       x2*x2      
    // x3         x1*x3       x2*x3       x3*x3      
    // x1*x1      x1*x1*x1    x2*x1*x1    x3*x1*x1    x1*x1*x1*x1  
    // x1*x2      x1*x1*x2    x2*x1*x2    x3*x1*x2    x1*x1*x1*x2  x1*x2*x1*x2  
    // x1*x3      x1*x1*x3    x2*x1*x3    x3*x1*x3    x1*x1*x1*x3  x1*x2*x1*x3  x1*x3*x1*x3  
    // x2*x2      x1*x2*x2    x2*x2*x2    x3*x2*x2    x1*x1*x2*x2  x1*x2*x2*x2  x1*x3*x2*x2  x2*x2*x2*x2  
    // x2*x3      x1*x2*x3    x2*x2*x3    x3*x2*x3    x1*x1*x2*x3  x1*x2*x2*x3  x1*x3*x2*x3  x2*x2*x2*x3  x2*x3*x2*x3  
    // x3*x3      x1*x3*x3    x2*x3*x3    x3*x3*x3    x1*x1*x3*x3  x1*x2*x3*x3  x1*x3*x3*x3  x2*x2*x3*x3  x2*x3*x3*x3  x3*x3*x3*x3 
     * 
     * Notice that matrix elements are combinations with repetitions of orders 
     * 0, 1, 2, 3 and 4 on set of N variables.
     * 
     * We operate on a set of integers {1, ... N} that correspond to variable
     * indices.
     * First we find all combinations with repetitions on the set {1, ... N}
     * up to the order N.
     * Next we partition each of these combinations into two subsets that are
     * mutually exclusive but together contain all the elements in the
     * combination, covering all possible partitions.
     * To form the above matrix, it suffices to partition the upper combinations
     * into two subsets - combinations of order up to 2. We assign an index to
     * each combination.
     * Again, for 3 variables, we would get:
     * Combinations WR of order up to 2, with their indices:
    ( 1 )  ->  1
    ( 2 )  ->  2
    ( 3 )  ->  3
    ( 1  1 )  ->  4
    ( 1  2 )  ->  5
    ( 1  3 )  ->  6
    ( 2  2 )  ->  7
    ( 2  3 )  ->  8
    ( 3  3 )  ->  9
     * Combinations WR of order up to 4 and their partitions into subsets.
     * Note that all partitions are covered and occur only once.
    ( 1 )
    ( 2 )
    ( 3 )
    ( 1 1 )
    ( 1 2 )
    ( 1 3 )
    ( 2 2 )
    ( 2 3 )
    ( 3 3 )
    ( 1 1 1 )  ->  ( ( 1 )( 1 1 ) )    
    ( 1 1 2 )  ->  ( ( 1 )( 1 2 ) )    ( ( 2 )( 1 1 ) )    
    ( 1 1 3 )  ->  ( ( 1 )( 1 3 ) )    ( ( 3 )( 1 1 ) )    
    ( 1 2 2 )  ->  ( ( 1 )( 2 2 ) )    ( ( 2 )( 1 2 ) )    
    ( 1 2 3 )  ->  ( ( 1 )( 2 3 ) )    ( ( 2 )( 1 3 ) )    ( ( 3 )( 1 2 ) )    
    ( 1 3 3 )  ->  ( ( 1 )( 3 3 ) )    ( ( 3 )( 1 3 ) )    
    ( 2 2 2 )  ->  ( ( 2 )( 2 2 ) )    
    ( 2 2 3 )  ->  ( ( 2 )( 2 3 ) )    ( ( 3 )( 2 2 ) )    
    ( 2 3 3 )  ->  ( ( 2 )( 3 3 ) )    ( ( 3 )( 2 3 ) )    
    ( 3 3 3 )  ->  ( ( 3 )( 3 3 ) )
    ( 1 1 1 1 )  ->  ( ( 1 )( 1 1 1 ) )    ( ( 1 1 )( 1 1 ) )    
    ( 1 1 1 2 )  ->  ( ( 1 )( 1 1 2 ) )    ( ( 1 1 )( 1 2 ) )    ( ( 1 2 )( 1 1 ) )    ( ( 2 )( 1 1 1 ) )    
    ( 1 1 1 3 )  ->  ( ( 1 )( 1 1 3 ) )    ( ( 1 1 )( 1 3 ) )    ( ( 1 3 )( 1 1 ) )    ( ( 3 )( 1 1 1 ) )    
    ( 1 1 2 2 )  ->  ( ( 1 )( 1 2 2 ) )    ( ( 1 1 )( 2 2 ) )    ( ( 1 2 )( 1 2 ) )    ( ( 2 )( 1 1 2 ) )    ( ( 2 2 )( 1 1 ) )    
    ( 1 1 2 3 )  ->  ( ( 1 )( 1 2 3 ) )    ( ( 1 1 )( 2 3 ) )    ( ( 1 2 )( 1 3 ) )    ( ( 1 3 )( 1 2 ) )    ( ( 2 )( 1 1 3 ) )    ( ( 2 3 )( 1 1 ) )    ( ( 3 )( 1 1 2 ) )    
    ( 1 1 3 3 )  ->  ( ( 1 )( 1 3 3 ) )    ( ( 1 1 )( 3 3 ) )    ( ( 1 3 )( 1 3 ) )    ( ( 3 )( 1 1 3 ) )    ( ( 3 3 )( 1 1 ) )    
    ( 1 2 2 2 )  ->  ( ( 1 )( 2 2 2 ) )    ( ( 1 2 )( 2 2 ) )    ( ( 2 )( 1 2 2 ) )    ( ( 2 2 )( 1 2 ) )    
    ( 1 2 2 3 )  ->  ( ( 1 )( 2 2 3 ) )    ( ( 1 2 )( 2 3 ) )    ( ( 1 3 )( 2 2 ) )    ( ( 2 )( 1 2 3 ) )    ( ( 2 2 )( 1 3 ) )    ( ( 2 3 )( 1 2 ) )    ( ( 3 )( 1 2 2 ) )    
    ( 1 2 3 3 )  ->  ( ( 1 )( 2 3 3 ) )    ( ( 1 2 )( 3 3 ) )    ( ( 1 3 )( 2 3 ) )    ( ( 2 )( 1 3 3 ) )    ( ( 2 3 )( 1 3 ) )    ( ( 3 )( 1 2 3 ) )    ( ( 3 3 )( 1 2 ) )    
    ( 1 3 3 3 )  ->  ( ( 1 )( 3 3 3 ) )    ( ( 1 3 )( 3 3 ) )    ( ( 3 )( 1 3 3 ) )    ( ( 3 3 )( 1 3 ) )    
    ( 2 2 2 2 )  ->  ( ( 2 )( 2 2 2 ) )    ( ( 2 2 )( 2 2 ) )    
    ( 2 2 2 3 )  ->  ( ( 2 )( 2 2 3 ) )    ( ( 2 2 )( 2 3 ) )    ( ( 2 3 )( 2 2 ) )    ( ( 3 )( 2 2 2 ) )    
    ( 2 2 3 3 )  ->  ( ( 2 )( 2 3 3 ) )    ( ( 2 2 )( 3 3 ) )    ( ( 2 3 )( 2 3 ) )    ( ( 3 )( 2 2 3 ) )    ( ( 3 3 )( 2 2 ) )    
    ( 2 3 3 3 )  ->  ( ( 2 )( 3 3 3 ) )    ( ( 2 3 )( 3 3 ) )    ( ( 3 )( 2 3 3 ) )    ( ( 3 3 )( 2 3 ) )    
    ( 3 3 3 3 )  ->  ( ( 3 )( 3 3 3 ) )    ( ( 3 3 )( 3 3 ) )     
     * 
     * Next we map each of the elements of partitions onto their indices, 
     * obtaining the coordinates where the combination, which was partitioned, 
     * should be put in our matrix.
     * e.g.
    ( 2 3 3 )  ->  ( ( 2 )( 3 3 ) )    ( ( 3 )( 2 3 ) )  ...partitions
    (x2*x3*x3)        ( 2     9 )        ( 3     8 )     ...coordinates
    So, x2*x3*x3 should be put in (2,9) and (3,8). (Note that this way we fill
    only one half of the matrix; to fill the other half we use the symmetric
    property of this matrix).
     * 
     * All of this can be generalized for a polynomial of arbitrary order.
     */
    /**
     * Generates combinations with repetition of elements that belong to set
     * @param set   
     * @param order
     * @return
     */
    public static List<List<Integer>> combinationsRepeating(List<Integer> set, int order) throws TooBig {

        List<List<Integer>> combis =
                new ArrayList<List<Integer>>();
        ArrayList<Integer> acc = new ArrayList<Integer>();
        List<Integer> restOfSet = new ArrayList();
        restOfSet.addAll(set);

        combinationsRepeatingHelper(restOfSet, acc, combis, order);
        Collections.sort(combis, new combiComparator());
        combis.add(0, null);   // combination of zeroth order

        return combis;
    }

    private static void combinationsRepeatingHelper(
            List<Integer> set, ArrayList<Integer> accumulator,
            List<List<Integer>> storage, int order) {

        ArrayList<Integer> acc = new ArrayList<Integer>();

        /*
         * 0-combi added in caller method
         */
        List<Integer> restOfSet = new ArrayList<Integer>(set);
        for (int i = 0; i < set.size(); i++) {
            acc = (ArrayList<Integer>) accumulator.clone();
            acc.add(set.get(i));

            if (acc.size() > order) {
                break;
            }
            combinationsRepeatingHelper(restOfSet, acc, storage, order);
            storage.add(acc);
            restOfSet.remove(0);
        }
    }

    /**
     * Generates combinations without repetition of elements that belong to set
     * @param set   
     * @param order
     * @return
     */
    public static List<List<Integer>> combinations(
            List<Integer> set, int order) throws TooBig {

        if (order > set.size()) {
            throw new TooBig("Order is greater than size of set");
        }

        List<List<Integer>> combis =
                new ArrayList<List<Integer>>();
        ArrayList<Integer> acc = new ArrayList<Integer>();

        combinationsHelper(set, acc, combis, order);
        Collections.sort(combis, new combiComparator());
        combis.add(0, null);   // combination of zeroth order

        return combis;
    }

    private static void combinationsHelper(
            List<Integer> set, ArrayList<Integer> accumulator,
            List<List<Integer>> storage, int order) {

        ArrayList<Integer> acc = new ArrayList<Integer>();

        /* combi of zeroth order added in caller method
         */
        List<Integer> restOfSet = new ArrayList<Integer>(set);

        for (int i = 0; i < set.size(); i++) {
            acc = (ArrayList<Integer>) accumulator.clone();
            acc.add(set.get(i));

            if (acc.size() > order) {
                break;
            }
            restOfSet.remove(0);
            storage.add(acc);
            combinationsHelper(restOfSet, acc, storage, order);
        }
    }

    /*
     * We want to take each of our combinations of order 4 and split them into
     * 2 subsets, mutually exclusive and together consisting the whole
     * combination, covering every possible split. We want these subsets to be
     * the elements of combinationsRepeating({1,2,...N},2).
     * No reason to do sth. like permutations in form of permutation cycles or
     * stuff and then filter them... simple and fast.
     */
    public static List<List<List<Integer>>> partitionCombinations(
            List<Integer> list) {

        List<List<List<Integer>>> newLists =
                new ArrayList<List<List<Integer>>>();
        List<Integer> temp1 = new ArrayList<Integer>();
        List<List<Integer>> temp2 = new ArrayList<List<Integer>>(2);

        temp1 = (ArrayList<Integer>) ((ArrayList<Integer>) list).clone();
        temp2.add(temp1);
        temp1 = new ArrayList<Integer>();
        //temp1.add(null);
        temp2.add(temp1);

        if (temp2.get(0).size() <= 2 && temp2.get(1).size() <= 2) {
            newLists.add(temp2);
        }

        partitionCombinationsHelper(temp2, newLists, 0);

        return newLists;
    }

    private static void partitionCombinationsHelper(
            List<List<Integer>> currentSplit,
            List<List<List<Integer>>> storage,
            int currentIndex) {

        ArrayList<Integer> temp1 = new ArrayList<Integer>();
        List<List<Integer>> temp2 = null;
        Integer currentInt;

        for (int i = currentIndex; i < currentSplit.get(0).size(); ++i) {
            temp1 = (ArrayList<Integer>) ((ArrayList<Integer>) currentSplit.get(0)).clone();
            currentInt = temp1.remove(i);
            if (i > 0) {
                if (currentInt == temp1.get(i - 1)) {
                    /* the same split has already occured so skip this one
                     */
                    continue;
                }
            }
            temp2 = new ArrayList<List<Integer>>(2);
            temp2.add(temp1);
            temp1 = (ArrayList<Integer>) ((ArrayList<Integer>) currentSplit.get(1)).clone();
            temp1.add(currentInt);
            temp2.add(temp1);
            if (temp2.get(0).size() <= 2 && temp2.get(1).size() <= 2) {
                storage.add(temp2);
            }
            partitionCombinationsHelper(temp2, storage, i);
        }
    }

    /**
     * Generates a list of all locations that need to be erased to
     * have all the combinations that contain n-th element from a list
     * of 1-combinations and 2-combinations removed.
     * 
     * @param setSize
     * @param n
     * @return
     */
    public static List<Integer> removeCR2List(int setSize, List<Integer> n)
            throws TooBig {

        if (n.get(n.size() - 1) > setSize) {
            throw new TooBig("n[n.size()-1] > setSize");
        }

        Collections.sort(n);

        List<Integer> coords = new ArrayList<Integer>();
        int[] m = new int[n.size()];
        int[] l = new int[n.size()];

        /*
         * have in mind that the zeroth member of list is null
         */

        /* initialization and a first add to coords
         */
        for (int i = 0; i < n.size(); ++i) {
            l[i] = n.get(i) - i;
            coords.add(l[i]);
            m[i] = setSize - n.size();
        }
        boolean mReducedInLastPass = true;

        int indexOfSmallest = 0;
        for (int i = 1; i <= n.get(n.size() - 1); ++i) {
            if (n.get(indexOfSmallest) == i) {
                /* special case - mark for deletion the whole block
                 */
                if (!mReducedInLastPass) {
                    for (int j = indexOfSmallest; j < n.size(); ++j) {
                        --m[j];
                    }
                    mReducedInLastPass = true;
                }
                l[indexOfSmallest] += m[indexOfSmallest];
                for (int j = 0; j < setSize - n.get(indexOfSmallest) + 1; ++j) {
                    coords.add(l[indexOfSmallest]);
                }
                ++indexOfSmallest;
            } else {
                for (int j = indexOfSmallest; j < n.size(); ++j) {
                    l[j] += m[j];
                    coords.add(l[j]);
                }
                mReducedInLastPass = false;
            }
        }
        return coords;
    }

    /**
     * Generates a list of all locations that need to be erased to
     * have all the combinations that contain n-th element from a list
     * of 1-combinations and 2-combinations removed. For arrays, that don't
     * get contracted after removal of an element, but the element becomes null
     * instead.
     * 
     * @param list
     * @param setSize
     * @param element
     * @return
     */
    static List<Integer> removeCR2Array(int setSize, List<Integer> n)
            throws TooBig {

        if (n.get(n.size() - 1) > setSize) {
            throw new TooBig("n[n.size()-1] > setSize");
        }

        Collections.sort(n);

        List<Integer> coords = new ArrayList<Integer>();
        int m;
        int[] l = new int[n.size()];

        /*
         * mind that the zeroth member of list is null
         */

        /* initialization and a first add to coords
         */
        for (int i = 0; i < n.size(); ++i) {
            l[i] = n.get(i);
            coords.add(l[i]);
        }
        m = setSize;

        int indexOfSmallest = 0;
        for (int i = 1; i <= n.get(n.size() - 1); ++i) {
            if (n.get(indexOfSmallest) == i) {
                /* special case - mark for deletion the whole block
                 */
                l[indexOfSmallest] += m;
                for (int j = 0; j < setSize - n.get(indexOfSmallest) + 1; ++j) {
                    coords.add(l[indexOfSmallest] + j);
                }
                for (int j = indexOfSmallest; j < n.size(); ++j) {
                    l[j] += m;
                }
                ++indexOfSmallest;
            } else {
                for (int j = indexOfSmallest; j < n.size(); ++j) {
                    l[j] += m;
                    coords.add(l[j]);
                }
            }
            --m;
        }
        return coords;
    }

    /**
     * Provided with a model, this method gives matrix and vectors related to
     * regression of a smaller model, whose links are a subset of original
     * model's links. The matrix and vector get deep copied.
     *
     * @param system    system of linear regression
     *                  equations for a polynomial network
     * @param toRemove  list of rows/clos we want to exclude from the original
     *                  model.
     *                  Suggested use: with removeCR2Array or removeCR2List
     * @return          contracted system of linear equations. does not return
     *                  the vector of unknowns
     */
    public static LinearEqSystem contractSystem(
            LinearEqSystem system, List<Integer> toRemove) {

        int contractDim = system.mA.length - toRemove.size();
        LinearEqSystem contractedSystem = new LinearEqSystem(contractDim);

        /*
         * we will copy blocks of doubles per rows with System.arrayCopy.
         * to do that, we need to calculate the index boundaries of those blocks
         * from the raw indices of columns in toRemove.
         * 
         * it would be somewhat faster were we to find that boundaries
         * directly in removeCR2Array, but done like this the code is more
         * flexible and it's easier to experiment with it
         */

        List<Integer[]> boundaries = new ArrayList<Integer[]>();

        Integer lastOne = -1;

        for (Integer i : toRemove) {

            if (i != lastOne + 1) {
                boundaries.add(new Integer[2]);
                boundaries.get(boundaries.size() - 1)[0] = lastOne + 1;
                boundaries.get(boundaries.size() - 1)[1] = i;
            }

            lastOne = i;
        }
        {
            Integer i = system.mA.length;
            if (i != lastOne + 1) {
                boundaries.add(new Integer[2]);
                boundaries.get(boundaries.size() - 1)[0] = lastOne + 1;
                boundaries.get(boundaries.size() - 1)[1] = i;
            }
        }

        int rowCounter = 0;      // contractedMatrix counter/iterator
        int colCounterV = 0;     // contractedVector counter/iterator
        for (Integer[] i : boundaries) {
            /* contract the matrix
             */
            for (int j = i[0]; j < i[1]; ++j) {
                int colCounter = 0;
                for (Integer[] k : boundaries) {
                    System.arraycopy(system.mA[j], k[0],
                            contractedSystem.mA[rowCounter], colCounter,
                            k[1] - k[0]);
                    colCounter += k[1] - k[0];
                }
                rowCounter++;
            }
            /* ... and contract the vector
             */
            System.arraycopy(system.mB_tr, i[0],
                    contractedSystem.mB_tr, colCounterV,
                    i[1] - i[0]);
            colCounterV += i[1] - i[0];
        }

        return contractedSystem;
    }

    /**
     * @param system        old system that will get extended
     * @param inputs        input values in points:
     *                      inputs[0][] to inputs[inputs.length-1][] are related
     *                      to old system;
     *                      double[inputs.length-1][] are the additional input
     *                      values
     * @param regressTo
     * @return
     */
    public static LinearEqSystem extendSystemByOne(LinearEqSystem system,
            double[][] inputs, double[] regressTo) throws TooBig {

        /*int setSize = (int) Math.round(
        (-1.5 + Math.sqrt(9 - 8 * (1 - system.mA.length)) / 2));
         */
        int numVariables = inputs.length - 1;     // number of variables in the
        // old system
        int numInstances = inputs[0].length;
        int numVariablesExtended = numVariables + 1;

        /*
         * Generate missing combinations using the already developed
         * combinatorial methods
         */
        List<Integer> sortedList = new ArrayList<Integer>();
        for (int i = 1; i <= numVariables; ++i) {
            sortedList.add(i);
        }

        List<List<Integer>> missingCombis =
                combinationsRepeating(sortedList, 3);
        missingCombis.set(0, new ArrayList<Integer>(1));

        for (List<Integer> combi : missingCombis) {
            combi.add(numVariablesExtended);
        }

        List<Integer> helper;
        for (int i = 0; i < missingCombis.size(); ++i) {
            if (missingCombis.get(i).size() < 4) {
                helper = (List<Integer>) ((ArrayList<Integer>) missingCombis.get(i)).clone();
                helper.add(numVariablesExtended);
                missingCombis.add(helper);
            }
        }

        /* expanded matrix/vectors size
         */
        int expandedSize = system.mB_tr.length + numVariablesExtended + 1;

        /* vectors and matrix of our future expanded system
         */
        double[][] equationCoefficients = new double[expandedSize][expandedSize];
        double[] rightSide = new double[expandedSize];
        double[] polyCoefficients = new double[expandedSize];

        /*
         * Partition the missing combis, find corresponding coordinates in
         * the regression matrix/vector, and calculate matrix/vector members
         */

        /* stores the ranges where to copy the original system's coefficient
         * matrix (also the right-side vector)
         */
        List<Integer> toProcess = new ArrayList<Integer>();

        for (int k = 0; k < missingCombis.size(); ++k) {
            /* partitions
             */
            List<List<List<Integer>>> splits =
                    partitionCombinations(missingCombis.get(k));

            for (int l = 0; l < splits.size(); ++l) {
                int[] coords = partitioningToCoordinates(
                        splits.get(l), numVariablesExtended);

                if (coords[0] == 0) {
                    toProcess.add(coords[1]);
                }

                switch (missingCombis.get(k).size()) {
                    case 1: {
                        int index1 = missingCombis.get(k).get(0) - 1;
                        for (int m = 0; m < numInstances; ++m) {
                            double temp = inputs[index1][m];
                            equationCoefficients[coords[0]][coords[1]] += temp;
                            if (coords[0] == 0) {
                                rightSide[coords[1]] += regressTo[m] * temp;
                            }
                        }
                        break;
                    }
                    case 2: {
                        int index1 = missingCombis.get(k).get(0) - 1;
                        int index2 = missingCombis.get(k).get(1) - 1;
                        for (int m = 0; m < numInstances; ++m) {
                            double temp = inputs[index1][m] *
                                    inputs[index2][m];
                            equationCoefficients[coords[0]][coords[1]] += temp;
                            if (coords[0] == 0) {
                                rightSide[coords[1]] += regressTo[m] * temp;
                            }
                        }
                        break;
                    }
                    case 3: {
                        int index1 = missingCombis.get(k).get(0) - 1;
                        int index2 = missingCombis.get(k).get(1) - 1;
                        int index3 = missingCombis.get(k).get(2) - 1;
                        for (int m = 0; m < numInstances; ++m) {
                            equationCoefficients[coords[0]][coords[1]] +=
                                    inputs[index1][m] *
                                    inputs[index2][m] *
                                    inputs[index3][m];
                        }
                        break;
                    }
                    case 4: {
                        int index1 = missingCombis.get(k).get(0) - 1;
                        int index2 = missingCombis.get(k).get(1) - 1;
                        int index3 = missingCombis.get(k).get(2) - 1;
                        int index4 = missingCombis.get(k).get(3) - 1;
                        for (int m = 0; m <
                                numInstances; ++m) {
                            equationCoefficients[coords[0]][coords[1]] +=
                                    inputs[index1][m] *
                                    inputs[index2][m] *
                                    inputs[index3][m] *
                                    inputs[index4][m];
                        }
                        break;
                    }
                }
            }
        }

        /*
         * Calculate boundaries for copying system array/vectors, the same thing
         * as in contractSystem()
         */
        List<Integer[]> boundaries = new ArrayList<Integer[]>();

        Integer lastOne = -1;

        for (Integer i : toProcess) {
            if (i != lastOne + 1) {
                boundaries.add(new Integer[2]);
                boundaries.get(boundaries.size() - 1)[0] = lastOne + 1;
                boundaries.get(boundaries.size() - 1)[1] = i;
            }

            lastOne = i;
        }

        int rowCounter = 0;     // contractedMatrix counter/iterator
        int colCounterV = 0;    // contractedVector counter/iterator
        for (Integer[] i : boundaries) {
            /* fill the rest of the matrix
             */
            for (int j = i[0]; j < i[1]; ++j) {
                int colCounter = 0;
                for (Integer[] k : boundaries) {
                    System.arraycopy(system.mA[rowCounter], colCounter,
                            equationCoefficients[j], k[0],
                            k[1] - k[0]);

                    colCounter += k[1] - k[0];
                }
                rowCounter++;
            }

            /* ... and the vector
             */
            System.arraycopy(system.mB_tr, colCounterV, rightSide,
                    i[0], i[1] - i[0]);
            colCounterV += i[1] - i[0];
        }

        /* Now we have a new, expanded system of linear equations
         */
        LinearEqSystem expandedSystem =
                new LinearEqSystem(equationCoefficients, rightSide,
                polyCoefficients);

        return expandedSystem;
    }

    /**
     * A comparator class used in sorting the combinations by length.
     */
    private static class combiComparator implements Comparator {

        public int compare(Object combi1, Object combi2) {
            if (((ArrayList<Integer>) combi1).size() >
                    ((ArrayList<Integer>) combi2).size()) {
                return 1;
            }
            if (((ArrayList<Integer>) combi1).size() <
                    ((ArrayList<Integer>) combi2).size()) {
                return -1;
            }
            /* Else it's equal
             */
            return 0;
        }
    }

    public static int[] partitioningToCoordinates(
            List<List<Integer>> partition, int nrVariables) {

        /* we assume that partition (a variable) is a valid partition and don't
         * check it
         */
        int[] coordinates = new int[2];
        for (int i = 0; i < 2; ++i) {
            switch (partition.get(i).size()) {
                case 1: {
                    coordinates[i] += partition.get(i).get(0);
                    break;
                }

                case 2: {
                    coordinates[i] = nrVariables +
                            (nrVariables + 1) * nrVariables / 2 -
                            (nrVariables - partition.get(i).get(0) + 2) *
                            (nrVariables - partition.get(i).get(0) + 1) / 2 +
                            partition.get(i).get(1) -
                            partition.get(i).get(0) + 1;
                    break;
                }
            }
        }

        return coordinates;
    }

    /**
     * Summand in the GMDH polynomial: coefficient*variable1*variable2*...
     */
    public static class Summand {

        public double coefficient;
        public ArrayList<Node> variables;

        Summand() {
            variables = new ArrayList<Node>();
        }
    }

    /**
     * i.e. model has following form:
     * P10 = a0 + a1*P1 + a2*P2 + a3*P3 +
     * a4*P1*P1 + a5*P1*P2 + a6*P1*P3 + a7*P2*P2 + a8*P2*P3 + a9*P3*P3,
     * method would return
     * [ [a0, [null]], [a1,[P1]], [a2,[P2]], [a3,[P3]], [a4,[P1,P1]], [a5,[P1,P2]], ...]
     *
     * @param coefficients
     * @return
     * @throws jGMDH.raw.exceptions.TooBig
     */
    public static ArrayList<Summand> generateSummands(Model m, double[] coefficients) {

        ArrayList<Summand> output = new ArrayList<Summand>();
        ArrayList<Integer> ints = new ArrayList<Integer>();
        for (int i = 0; i < m.links.size(); ++i) {
            ints.add(i);
        }
        List<List<Integer>> combinations = null;
        try {
            combinations = wGmdh.jGmdh.hybrid.SseRegressionEquations.combinationsRepeating(ints, 2);
        } catch (TooBig ex) {
            Logger.getLogger(SseRegressionEquations.class.getName()).log(Level.SEVERE, null, ex);
        }

        int counter = 0;

        Summand summand = new Summand();
        summand.coefficient = coefficients[counter++];
        output.add(summand);

        Iterator<List<Integer>> combinationsIter = combinations.iterator();
        combinationsIter.next();    // skip zeroth combination, it is null

        /* Pass through all combinations
         */
        while (combinationsIter.hasNext()) {
            Summand summand2 = new Summand();
            summand2.coefficient = coefficients[counter];
            Iterator<Integer> variablesIter = combinationsIter.next().iterator();
            while (variablesIter.hasNext()) {
                summand2.variables.add(m.links.get(variablesIter.next()));
            }
            /*
            Iterator<Node> printoutIter = summand2.variables.iterator();
            System.out.println("cnt: " + counter);
            System.out.println("    coeff: " + summand2.coefficient);
            System.out.println("    variables: ");
            while (printoutIter.hasNext()) {
            System.out.print(printoutIter.next().getIdentifier() + " ");
            System.out.println();
            }
             */
            ++counter;
            output.add(summand2);

        }
        return output;
    }
}