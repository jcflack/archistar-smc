package at.archistar.crypto.math;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;

/**
 * generic matrix implementation only depending upon a field
 */
public class GenericMatrix implements GFMatrix {
    
    private final int[][] matrix;
    
    private final GF gf;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public GenericMatrix(int input[][], GF gf) {
        this.gf = gf;
        this.matrix = input;
    }
 
    @Override
    public GFMatrix inverse() {
        return this.inverse(true);
    }

    @Override
    public int[] rightMultiply(int[] vec) {
        if (vec.length != matrix.length || vec.length != matrix[0].length) { // multiplication only works if A(MxN) and B(NxO)
            throw new ArithmeticException("when matrix is MxN, vector must be Nx1"); 
        }

        int[] result = new int[vec.length];
        for (int i = 0; i < vec.length; i++) {
            int tmp = 0;
            for (int j = 0; j < vec.length; j++) {
                tmp = gf.add(tmp, gf.mult(matrix[i][j], vec[j]));
            }
            result[i] = tmp;
        }
        return result;
    }
    
    /* where is the dead store? */
    private GFMatrix inverse(boolean throwException) {
        
        int numRows = matrix.length;
        
        // clone this matrix
        int[][] tmpMatrix = new int[numRows][];
        for (int i = numRows - 1; i >= 0; i--) {
            tmpMatrix[i] = Arrays.copyOf(matrix[i], matrix[i].length);
        }

        // initialize inverse matrix as unit matrix
        int[][] invMatrix = new int[numRows][numRows];
        for (int i = numRows - 1; i >= 0; i--) {
            invMatrix[i][i] = 1;
        }

        // simultaneously compute Gaussian reduction of tmpMatrix and unit matrix
        for (int i = 0; i < numRows; i++) {
            // if diagonal element is zero
            if (tmpMatrix[i][i] == 0) {
                boolean foundNonZero = false;
                // find a non-zero element in the same column
                for (int j = i + 1; j < numRows; j++) {
                    if (tmpMatrix[j][i] != 0) {
                        // found it, swap rows ...
                        foundNonZero = true;
                        swapRows(tmpMatrix, i, j);
                        swapRows(invMatrix, i, j);
                        // ... and quit searching
                        j = numRows;
                    }
                }
                // if no non-zero element was found
                if (!foundNonZero) {
                    if (throwException) {
                        throw new RuntimeException("blub");
                    } else {
                        // this row is dependent so eliminate it with the corresponding column
                        numRows--; // this will only happen in the last row
                    }
                }
            }

            // normalize i-th row
            int coef = tmpMatrix[i][i];
            int invCoef = gf.inverse(coef);
            
            normalizeRow(tmpMatrix[i], invMatrix[i], invCoef);

            // subtract from all other rows
            for (int j = 0; j < numRows; j++) {
                if (j != i) {
                    coef = tmpMatrix[j][i];
                    if (coef != 0) {
                        multAndSubstract(tmpMatrix[j], tmpMatrix[i], coef);
                        multAndSubstract(invMatrix[j], invMatrix[i], coef);
                    }
                }
            }
        }
        
        return new GenericMatrix(invMatrix, gf);        
    }
    
    private void multAndSubstract(int[] row, int[] normalized, int coef) {
        for (int i = 0; i < row.length; i++) {
            row[i] = gf.sub(row[i], gf.mult(normalized[i], coef));
        }
    }

    @Override
    public GFMatrix inverseElimDepRows() {
        return this.inverse(false);
    }
    
    /*
     * Helper-methods from for Gaussian elimination
     * @author flexiprovider
     */
    private static void swapRows(int[][] matrix, int first, int second) {
        int[] tmp = matrix[first];
        matrix[first] = matrix[second];
        matrix[second] = tmp;
    }
    
    @Override
    public int getNumRows() {
        return this.matrix.length;
    }

    private void normalizeRow(int[] tmpMatrix, int[] invMatrix, int element) {
        for (int i = tmpMatrix.length - 1; i >= 0; i--) {
            tmpMatrix[i] = gf.mult(tmpMatrix[i], element);
            invMatrix[i] = gf.mult(invMatrix[i], element);
        }
    }
}
