package org.eclipse.dataspaceconnector.checkstyle;

import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

import static java.lang.String.format;

public class ProhibitFinalModifier extends AbstractCheck {
    private boolean checkMethodParameters = true;
    private boolean checkLocalVariables = true;


    @Override
    public int[] getDefaultTokens() {
        return new int[]{ TokenTypes.VARIABLE_DEF, TokenTypes.PARAMETER_DEF };
    }

    @Override
    public int[] getAcceptableTokens() {
        return getDefaultTokens();
    }

    @Override
    public int[] getRequiredTokens() {
        return getDefaultTokens();
    }

    @Override
    public void visitToken(DetailAST ast) {
        if (ast.getType() == TokenTypes.PARAMETER_DEF && checkMethodParameters) {
            if (ast.findFirstToken(TokenTypes.MODIFIERS).findFirstToken(TokenTypes.FINAL) != null) {

                DetailAST typeAst = ast.findFirstToken(TokenTypes.TYPE).getFirstChild();

                DetailAST nameAst = ast.findFirstToken(TokenTypes.IDENT);
                String name = nameAst.getText();
                log(ast, format("Found method parameter declared as final: \"final %s %s\"", typeAst.getText(), name));
            }
        } else if (ast.getType() == TokenTypes.VARIABLE_DEF && checkLocalVariables) {
            if (ast.getParent().getType() != TokenTypes.OBJBLOCK && ast.findFirstToken(TokenTypes.MODIFIERS).findFirstToken(TokenTypes.FINAL) != null) { // means not a class member
                DetailAST typeAst = ast.findFirstToken(TokenTypes.TYPE).getFirstChild();

                DetailAST nameAst = ast.findFirstToken(TokenTypes.IDENT);
                String name = nameAst.getText();

                log(ast, format("Found local variable declared as final: \"final %s %s\"", typeAst.getText(), name));
            }
        }
    }

    @Override
    public void leaveToken(DetailAST ast) {
        super.leaveToken(ast);
    }

    public boolean isCheckMethodParameters() {
        return checkMethodParameters;
    }

    public void setCheckMethodParameters(boolean checkMethodParameters) {
        this.checkMethodParameters = checkMethodParameters;
    }

    public void setCheckLocalVariables(boolean checkLocalVariables) {
        this.checkLocalVariables = checkLocalVariables;
    }
}

