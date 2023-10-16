package team.todaybest.analyser.model;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.PackageDeclaration;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Java Package的抽象描述
 *
 * @author cineazhan
 */
@Data
public class JavaPackage {
    private PackageDeclaration packageDeclaration;
    private List<JavaFile> files;

    private List<JavaPackage> childrenPackages;
    private JavaPackage parentPackage;

    public JavaPackage(){
        this.files = new ArrayList<>();
        this.childrenPackages = new ArrayList<>();
    }
}
