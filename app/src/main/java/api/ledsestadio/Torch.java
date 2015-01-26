package api.ledsestadio;

import android.hardware.Camera;
import android.hardware.Camera.Parameters;

/**
 * Created by enriquesolis on 21/01/15.
 */
public class Torch {

    private Camera camera;

    private Parameters parameters;

    private boolean on;


    public Torch()
    {
        camera = Camera.open();
        parameters = camera.getParameters();
        camera.startPreview();
        on = false;
    }


    public void on()
    {
        if (!on)
        {
            on = true;
            parameters.setFlashMode(Parameters.FLASH_MODE_TORCH);
            camera.setParameters(parameters);
        }
    }

    public void off()
    {
        if (on)
        {
            on = false;
            parameters.setFlashMode(Parameters.FLASH_MODE_OFF);
            camera.setParameters(parameters);
        }
    }

    public void release()
    {
        camera.stopPreview();
        camera.release();
    }

    public boolean isOn()
    {
        // Opci�n alternativa que nos ahorrar�a el uso de la
        // variable on.
        // return (parameters.getFlashMode() ==
        //         Parameters.FLASH_MODE_TORCH);
        return on;
    }
}
