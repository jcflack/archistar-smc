package at.archistar.crypto.math.bc;

import at.archistar.crypto.math.GF;
import at.archistar.crypto.math.GFFactory;
import at.archistar.crypto.math.GFMatrix;
import at.archistar.crypto.math.GenericMatrix;

/**
 *
 * @author andy
 */
public class BCGFFactory implements GFFactory {
    
    private static final BCGF256 gf256 = new BCGF256();
    
    static {
        System.err.println("creating new BCGFfactory!");
    }
    
    @Override
    public GF createHelper() {
        return gf256;
    }
    
    @Override
    public GFMatrix createMatrix(int matrix[][]) {
        return new GenericMatrix(matrix, gf256);
    }
}
