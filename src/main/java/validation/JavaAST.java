package validation;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.utils.CodeGenerationUtils;
import com.github.javaparser.utils.SourceRoot;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class JavaAST {
    protected final EccoNode root;
    protected final EccoSet<EccoNode> astNodes;
    protected final Set<String> fileTypes;

    public JavaAST() {
        this.fileTypes = new HashSet<>();
        this.fileTypes.add(".java");
        root = new EccoNode(null, null, RootPosition.INSTANCE, EccoNode.NODE_TYPE.ROOT, null);
        astNodes = new EccoSet<>();
    }

    public JavaAST(final File file) {
        this.fileTypes = new HashSet<>();
        this.fileTypes.add(".java");
        root = new EccoNode(null, null, RootPosition.INSTANCE, EccoNode.NODE_TYPE.ROOT, null);
        visitFile(root, file);
        astNodes = collectAstNodes();
    }

    public JavaAST(final EccoNode root, final EccoSet<EccoNode> astNodes) {
        this.fileTypes = new HashSet<>();
        this.fileTypes.add(".java");
        this.root = root;
        this.astNodes = astNodes;
    }

    // collects all nodes (except the root node) of the AST in one set to simplify their access
    public EccoSet<EccoNode> collectAstNodes() {
        final EccoSet<EccoNode> result = new EccoSet<>();
        final ArrayList<EccoNode> nodesToVisit = new ArrayList<>();
        nodesToVisit.add(this.getRoot());
        while (!nodesToVisit.isEmpty()) {
            result.add(nodesToVisit.get(0));
            nodesToVisit.addAll(nodesToVisit.remove(0).getChildren());
        }
        result.remove(root);
        return result;
    }

    //create the AST from a file
    private void visitFile(final EccoNode parent, final File file) {
            if (file.isFile()) {
                final EccoNode fileNode = new EccoNode(parent, file.getName(), new FilePosition(file.toString()), EccoNode.NODE_TYPE.FILE, null);
                parent.addChild(fileNode);
                if (fileTypes.stream().anyMatch(t -> file.getAbsolutePath().endsWith(t))) {
                    visitFileContent(fileNode, file);
                }
            } else {
                System.out.println("File error, not a file");
            }
    }

    public EccoNode getRoot() {
        return root;
    }

    public EccoSet<EccoNode> getAstNodes() {
        return astNodes;
    }

    protected void visitFileContent(final EccoNode fileNode, final File fileToVisit) {
        final SourceRoot sourceRoot = new SourceRoot(CodeGenerationUtils.mavenModuleRoot(JavaAST.class).resolve(fileToVisit.getParentFile().getAbsolutePath()));
        final CompilationUnit cu = sourceRoot.parse("", fileToVisit.getName());
        cu.accept(new JavaVisitor(fileToVisit.toString()), fileNode);
    }
}
