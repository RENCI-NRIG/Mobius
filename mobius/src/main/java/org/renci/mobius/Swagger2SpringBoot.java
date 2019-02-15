package org.renci.mobius;

import org.renci.mobius.controllers.MobiusController;
import org.renci.mobius.service.WorkflowService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.beans.factory.annotation.Autowired;

import springfox.documentation.swagger2.annotations.EnableSwagger2;

@SpringBootApplication
@EnableSwagger2
@ComponentScan(basePackages = { "org.renci.mobius", "org.renci.mobius.api" , "org.renci.mobius.config"})
public class Swagger2SpringBoot implements CommandLineRunner {

    @Autowired
    private WorkflowService service;

    @Override
    public void run(String... arg0) throws Exception {
        if (arg0.length > 0 && arg0[0].equals("exitcode")) {
            throw new ExitException();
        }
        System.out.println("===========================================MOBIUS STARTUP BEGIN===========================");
        System.out.println("===========================================Using service = " + service);
        MobiusController.getInstance().setService(service);
        MobiusController.startThreads();
        System.out.println("===========================================MOBIUS STARTUP COMPLETE========================");
    }

    public static void main(String[] args) throws Exception {
        new SpringApplication(Swagger2SpringBoot.class).run(args);
    }

    class ExitException extends RuntimeException implements ExitCodeGenerator {
        private static final long serialVersionUID = 1L;

        @Override
        public int getExitCode() {
            return 10;
        }

    }
}
