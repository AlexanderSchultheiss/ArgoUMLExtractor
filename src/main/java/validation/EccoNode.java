package validation;

import org.logicng.formulas.Formula;
import java.io.Serializable;
import java.util.Objects;

public class EccoNode implements Serializable {

    public enum NODE_TYPE {
        ROOT, FOLDER, FILE, LINE, DEFAULT,
        CLASS_OR_INTERFACE_DECLARATION, METHOD_DECLARATION, IF_STATEMENT,
        ELSE_STATEMENT, THEN_STATEMENT, CONSTRUCTOR_DECLARATION,
        FOREACH_STATEMENT, FOR_STATEMENT, DO_STATEMENT, ENUM_CONSTANT_DECLARATION, ENUM_DECLARATION,
        SWITCH_ENTRY, SWITCH_STMT, MODULE_DECLARATION,
    }

    private final String code;
    private EccoNode parent;
    private final EccoSet<EccoNode> children;
    private final NODE_TYPE type;
    private final transient Position startPosition;
    private int sequenceNumber = 0;

    public EccoNode(final EccoNode parent, final String code, final Position position, final NODE_TYPE type, final Formula mapping) {
        this.parent = parent;
        this.code = code;
        this.startPosition = Objects.requireNonNull(position);
        this.type = type;
        children = new EccoSet<>();
    }

    //if two nodes are at the same position of the AST and contain the same code, we distinguish them by sequence numbers
    /*Warning: Later on we will match nodes from different products by their sequence numbers, but that is not necessarily correct (they can e.g. be shifted by inserts or deletes).
    Correct matching of the sequence numbers would require further investigation of the nodes' children and was omitted here.
     */
    public void addChild(final EccoNode child) {
        while (!children.add(child)) {
            child.sequenceNumber += 1;
        }
    }

    public boolean isSimilar(final EccoNode eccoNode) {
        if (this == eccoNode) return true;
        if (eccoNode == null || getClass() != eccoNode.getClass()) return false;
        return Objects.equals(code, eccoNode.code) &&
                this.similarParent(eccoNode) &&
                type == eccoNode.type &&
                sequenceNumber == eccoNode.sequenceNumber;
    }

    public String getCode() {
        return code;
    }

    private String getAncestorCode() {
        if (this.parent == null) {
            return null;
        } else {
            if (this.parent.code == null) {
                return this.parent.getAncestorCode();
            } else {
                return this.parent.code;
            }
        }
    }

    private boolean similarParent(final EccoNode other) {
        if (this.parent == other.parent) return true;
        if (this.parent == null || this.parent.getClass() != other.parent.getClass()) return false;
        return Objects.equals(this.parent.code, other.parent.code) &&
                this.parent.type == other.parent.type &&
                this.parent.sequenceNumber == other.parent.sequenceNumber;
    }

    public EccoNode getParent() {
        return parent;
    }

    public void setParent(final EccoNode parent) {
        this.parent = parent;
    }

    public EccoSet<EccoNode> getChildren() {
        return children;
    }

    public Position getStartPosition() {
            return startPosition;
    }

    public NODE_TYPE getType() {
        return type;
    }

}
