package team.todaybest.analyser;

import org.fusesource.jansi.AnsiConsole;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author cineazhan
 */
@SpringBootApplication
public class JavaProjectAnalyserCliApplication {

    public static void main(String[] args) {
        AnsiConsole.systemInstall();
        SpringApplication.run(JavaProjectAnalyserCliApplication.class, args);
    }

}
