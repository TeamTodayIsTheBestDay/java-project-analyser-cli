package team.todaybest.analyser.service.impl;

import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.NodeList;
import org.springframework.stereotype.Service;
import team.todaybest.analyser.service.ImportService;

import java.util.Optional;

/**
 * @author cinea
 */
@Service
public class ImportServiceImpl implements ImportService {
    @Override
    public Optional<String> getReferenceFromName(NodeList<ImportDeclaration> importDeclarations, String identifier) {
        return importDeclarations.stream()
                .filter(importDeclaration -> importDeclaration.getName().getIdentifier().equals(identifier))
                .map(importDeclaration -> importDeclaration.getName().getQualifier()+"."+importDeclaration.getName().getIdentifier())
                .findFirst();
    }

    @Override
    public boolean referenceExists(NodeList<ImportDeclaration> importDeclarations, String classReference) {
        return importDeclarations.stream()
                .anyMatch(importDeclaration -> importDeclaration.getNameAsString().equals(classReference));
    }
}
