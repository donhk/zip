package com.donhk.boot;

import com.donhk.config.CliValidator;
import com.donhk.config.SetupBuilder;

public class Main {
    public static void main(String[] args) {
        try {
            CliValidator cliValidator = new CliValidator(args);
            if (!cliValidator.validate()) {
                System.exit(-1);
            }

            SetupBuilder setupBuilder = new SetupBuilder(cliValidator.getSettings());
            if (cliValidator.getSettings().isDecompress()) {
                setupBuilder.decompress();
            } else {
                setupBuilder.compress();
            }

        } catch (InterruptedException e) {
            System.err.println("The tasks was cancelled");
            System.exit(-1);
        } catch (Exception e) {
            System.err.println("There was a problem executing zipper :(");
            System.exit(-1);
        }

    }


}
