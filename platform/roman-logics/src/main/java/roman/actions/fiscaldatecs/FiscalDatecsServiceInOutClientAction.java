package roman.actions.fiscaldatecs;

import platform.interop.action.ClientAction;
import platform.interop.action.ClientActionDispatcher;
import platform.interop.action.MessageClientAction;

import java.io.IOException;
import java.util.List;


public class FiscalDatecsServiceInOutClientAction implements ClientAction {

    int baudRate;
    int comPort;
    Double sum;

    public FiscalDatecsServiceInOutClientAction(Integer baudRate, Integer comPort, Double sum) {
        this.baudRate = baudRate == null ? 0 : baudRate;
        this.comPort = comPort == null ? 0 : comPort;
        this.sum = sum;
    }


    public Object dispatch(ClientActionDispatcher dispatcher) throws IOException {

        try {
            FiscalDatecs.init();

            FiscalDatecs.openPort(comPort, baudRate);

            FiscalDatecs.inOut(sum);

            FiscalDatecs.closePort();

            FiscalDatecs.closeWriter();

        } catch (RuntimeException e) {
            return FiscalDatecs.getError();
        }
        return null;
    }
}
