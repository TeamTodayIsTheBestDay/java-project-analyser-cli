package team.todaybest.analyser.service;

/**
 * @author cineazhan
 */
public interface AnalysisService {
    void openProject(String path);

    void listProject();

    void getInstances(String classReference);

    void methodRelationship(String classReference, String functionName, int depth);

    void methodParameterOrigin(String classReference, String functionName, int depth);
}
