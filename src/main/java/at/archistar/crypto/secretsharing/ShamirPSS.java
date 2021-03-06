package at.archistar.crypto.secretsharing;

import at.archistar.crypto.data.BaseShare;
import at.archistar.crypto.data.ByteUtils;
import at.archistar.crypto.data.InvalidParametersException;
import at.archistar.crypto.data.ShamirShare;
import at.archistar.crypto.data.Share;
import at.archistar.crypto.decode.Decoder;
import at.archistar.crypto.decode.DecoderFactory;
import at.archistar.crypto.decode.ErasureDecoder;
import at.archistar.crypto.decode.ErasureDecoderFactory;
import at.archistar.crypto.decode.UnsolvableException;
import at.archistar.crypto.exceptions.ImpossibleException;
import at.archistar.crypto.exceptions.ReconstructionException;
import at.archistar.crypto.exceptions.WeakSecurityException;
import at.archistar.crypto.math.GF;
import at.archistar.crypto.math.GFFactory;
import at.archistar.crypto.math.gf256.GF256Factory;
import at.archistar.crypto.random.RandomSource;
import java.util.Arrays;

/**
 * <p>This class implements the <i>Perfect-Secret-Sharing</i>-scheme (PSS) developed by Adi Shamir.</p>
 * 
 * <p>For a detailed description of the scheme, 
 * see: <a href='http://en.wikipedia.org/wiki/Shamir's_Secret_Sharing'>http://en.wikipedia.org/wiki/Shamir's_Secret_Sharing</a></p>
 */
public class ShamirPSS extends SecretSharing {
    private final RandomSource rng;
    private final DecoderFactory decoderFactory;
    
    private final GF gf;
    
    private static final GFFactory defaultGFFactory = new GF256Factory();
    
    /**
     * Constructor
     * <p>(applying {@link ErasureDecoder} as default reconstruction algorithm)</p>
     * 
     * @param n the number of shares to create
     * @param k the minimum number of shares required for reconstruction
     * @param rng the source of randomness to use for generating the coefficients
     * @throws WeakSecurityException thrown if this scheme is not secure enough for the given parameters
     */
    public ShamirPSS(int n, int k, RandomSource rng) throws WeakSecurityException {
        this(n, k, rng, new ErasureDecoderFactory(defaultGFFactory), defaultGFFactory.createHelper());
    }
    
    /**
     * Constructor
     * 
     * @param n the number of shares to create
     * @param k the minimum number of shares required for reconstruction
     * @param rng the source of randomness to use for generating the coefficients
     * @param decoderFactory the solving algorithm to use for reconstructing the secret
     * @throws WeakSecurityException thrown if this scheme is not secure enough for the given parameters
     */
    public ShamirPSS(int n, int k, RandomSource rng, DecoderFactory decoderFactory) throws WeakSecurityException {
        this(n, k, rng, decoderFactory, defaultGFFactory.createHelper());
    }
    
    /**
     * Constructor
     * 
     * @param n the number of shares to create
     * @param k the minimum number of shares required for reconstruction
     * @param rng the source of randomness to use for generating the coefficients
     * @param decoderFactory the solving algorithm to use for reconstructing the secret
     * @throws WeakSecurityException thrown if this scheme is not secure enough for the given parameters
     */
    public ShamirPSS(int n, int k, RandomSource rng, DecoderFactory decoderFactory, GF gf) throws WeakSecurityException {
        super(n, k);
        
        this.rng = rng;
        this.decoderFactory = decoderFactory;
        this.gf = gf;
    }

    @Override
    public Share[] share(byte[] data) {
        try {
            ShamirShare shares[] = createShamirShares(n, data.length);

            /* calculate the x and y values for the shares */
            for (int i = 0; i < data.length; i++) {
                int[] poly = createShamirPolynomial(ByteUtils.toUnsignedByte(data[i]), k-1); // generate a new random polynomial
            
                for (ShamirShare share : shares) { // evaluate the x-values at the polynomial
                    share.getY()[i] = (byte) gf.evaluateAt(poly, share.getId());
                }
            }
            return shares;
        } catch (InvalidParametersException ex) {
            throw new ImpossibleException("sharing failed (" + ex.getMessage() + ")");
        }
    }

    @Override
    public byte[] reconstruct(Share[] shares) throws ReconstructionException {
        if (!validateShareCount(shares.length, k)) {
            throw new ReconstructionException();
        }

        /* you cannot cast arrays to arrays of subtype in java7 */
        ShamirShare[] sshares = Arrays.copyOf(shares, shares.length, ShamirShare[].class); // we need access to the inner fields
        
        byte[] result = new byte[sshares[0].getY().length];
        int[] xVals = BaseShare.extractXVals(sshares);
        
        Decoder decoder = decoderFactory.createDecoder(xVals, k);
        for (int i = 0; i < result.length; i++) { // reconstruct all individual parts of the secret
            int[] yVals = ShamirShare.extractYVals(sshares, i);
            
            try {
                result[i] = (byte) decoder.decode(yVals, 0)[0];
            } catch (UnsolvableException e) {
                throw new ReconstructionException("too few shares to reconstruct");
            }
        }   
        
        return result;
    }
    
    /**
     * Creates a new polynomial for Shamir-Secret-Sharing.<br>
     * In other words a polynomials with <i>degree</i> random coefficients and secret as the constant coefficient.
     * 
     * @param secret the secret to share (the constant coefficient)
     * @param degree the degree of the polynomial (number of random coefficients, must be <i>k</i>)
     * @return a random polynomial with the specified parameters ready for sharing the secret
     */
    private int[] createShamirPolynomial(int secret, int degree) {
        int[] coeffs = new int[degree + 1];
        
        this.rng.fillBytesAsInts(coeffs);
        coeffs[0] = secret;
        return coeffs;
    }

    /**
     * Creates <i>n</i> ShamirShares with the given share-length.
     * 
     * @param n the number of ShamirShares to create
     * @param shareLength the length of all shares
     * @return an array with the created shares
     */
    public static ShamirShare[] createShamirShares(int n, int shareLength) throws InvalidParametersException {
        ShamirShare[] sshares = new ShamirShare[n];
        
        for (int i = 0; i < n; i++) {
            sshares[i] = new ShamirShare((byte) (i+1), new byte[shareLength]);
        }
        
        return sshares;
    }
    
    @Override
    public String toString() {
        return "ShamirPSS(" + n + "/" + k + ")";
    }
}
