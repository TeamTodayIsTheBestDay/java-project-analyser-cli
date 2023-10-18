package team.todaybest.analyser.service;

/**
 * @author cineazhan
 */
public interface AnalysisService {
    void openProject(String path);

    void listProject();

    void getInstances(String classReference);

    void functionRelationship(String classReference, String functionName);
}
