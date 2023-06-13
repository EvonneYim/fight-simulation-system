
package FMS;

import java.util.concurrent.Callable;

public class CabinLoss {

    public boolean loss = false;
}

class Logic implements Callable<CabinLoss> {

    CabinLoss cl;

    public Logic(CabinLoss cl) {
        this.cl = cl;
    }

    @Override
    public CabinLoss call() throws Exception {
        
        cl.loss = true;
        
        return cl;
    }

}

