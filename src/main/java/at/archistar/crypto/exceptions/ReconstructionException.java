package at.archistar.crypto.exceptions;

/**
 * Exception that is thrown when the reconstruction of a secret failed.
 *
 * @author Fehrenbach Franca-Sofia
 * @version 2014-7-21
 */
public class ReconstructionException extends Exception {
    private static final long serialVersionUID = 1L;
    
    private String msg;

    public ReconstructionException() {
        this.msg = "generic";
    }
    
    public ReconstructionException(String msg) {
        this.msg = msg;
    }
}
