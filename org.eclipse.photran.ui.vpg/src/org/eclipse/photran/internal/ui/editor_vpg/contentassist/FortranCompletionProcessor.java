/*******************************************************************************
 * Copyright (c) 2009, 2014 University of Illinois at Urbana-Champaign and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     UIUC - Initial API and implementation
 *     Chris Hansen (U Washington) - Auto-complete improvements (Bug 414906)
 *******************************************************************************/
package org.eclipse.photran.internal.ui.editor_vpg.contentassist;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.photran.internal.core.analysis.binding.Definition;
import org.eclipse.photran.internal.core.analysis.binding.Definition.Classification;
import org.eclipse.photran.internal.core.analysis.binding.ScopingNode;
import org.eclipse.photran.internal.core.analysis.types.DerivedType;
import org.eclipse.photran.internal.core.analysis.types.Type;
import org.eclipse.photran.internal.core.lexer.Token;
import org.eclipse.photran.internal.core.parser.ASTDerivedTypeDefNode;
import org.eclipse.photran.internal.core.parser.ASTDerivedTypeStmtNode;
import org.eclipse.photran.internal.core.parser.ASTTypeAttrSpecNode;
import org.eclipse.photran.internal.core.parser.IASTListNode;
import org.eclipse.photran.internal.core.properties.SearchPathProperties;
import org.eclipse.photran.internal.core.vpg.PhotranTokenRef;
import org.eclipse.photran.internal.core.vpg.PhotranVPG;
import org.eclipse.photran.internal.ui.editor.FortranEditor;
import org.eclipse.photran.internal.ui.editor.FortranStmtPartitionScanner;
import org.eclipse.photran.internal.ui.editor.FortranTemplateCompletionProcessor;
import org.eclipse.photran.internal.ui.editor_vpg.FortranEditorTasks;
import org.eclipse.photran.internal.ui.editor_vpg.contentassist.FortranCompletionProposalComputer.Context;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;

/**
 * Fortran content assist processor which uses the VPG to determine which identifiers are in scope.
 * 
 * @author Jeff Overbey
 * @author Chris Hansen
 */
public class FortranCompletionProcessor implements IContentAssistProcessor
{
    /** Scope map: scopes.get(n) is the qualified name of the scope at line (n+1) */
    ArrayList<String> scopes = new ArrayList<String>();
    
    /** Maps qualified scope names to lists of definitions declared in that scope */
    HashMap<String, TreeSet<Definition>> defs = new HashMap<String, TreeSet<Definition>>();
    
    private String errorMessage = null;

    //private final Color LIGHT_YELLOW = new Color(null, new RGB(255, 255, 191));
    private final Color LIGHT_YELLOW = new Color(null, new RGB(255, 255, 223));
    //private final Color WHITE = new Color(null, new RGB(255, 255, 255));

    public IContentAssistant setup(FortranEditor editor)
    {
        String contentAssistEnabledProperty = new SearchPathProperties().getProperty(
            editor.getIFile(),
            SearchPathProperties.ENABLE_CONTENT_ASSIST_PROPERTY_NAME);
        if (contentAssistEnabledProperty != null && contentAssistEnabledProperty.equals("true")) //$NON-NLS-1$
        {
            FortranEditorTasks.instance(editor).addASTTask(new FortranCompletionProcessorASTTask(this));
            FortranEditorTasks.instance(editor).addVPGTask(new FortranCompletionProcessorVPGTask(this));
            
            ContentAssistant assistant = new ContentAssistant();
            for (String partitionType : FortranStmtPartitionScanner.PARTITION_TYPES)
                assistant.setContentAssistProcessor(this, partitionType);
            assistant.enableAutoActivation(false); //assistant.setAutoActivationDelay(500);
            assistant.setProposalPopupOrientation(IContentAssistant.CONTEXT_INFO_BELOW);
            assistant.setContextInformationPopupBackground(LIGHT_YELLOW);
            assistant.setProposalSelectorBackground(LIGHT_YELLOW);
            return assistant;
        }
        else return null;
    }

    public synchronized ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset)
    {
        FortranCompletionProposalComputer computer = null;
        List<ICompletionProposal> proposals = new ArrayList<ICompletionProposal>(256);
        
        if (defs != null)
        {
    //        return new ICompletionProposal[]
    //        {
    //            new CompletionProposal("Hello", offset, 0, 5),
    //            new CompletionProposal("Goodbye", offset, 0, 7)
    //        };
            
            try
            {
                errorMessage = null;
                
                IDocument document = viewer.getDocument();
                
                int line = determineLineNumberForOffset(offset, document);
                String scopeName = determineScopeNameForLine(line);
                Context contextType = determineContext(offset,line,document);
                List<Definition> classDefs = null;
                if (contextType == Context.USE) {
                    classDefs = determineModuleNames();
                    if (classDefs.isEmpty())
                        classDefs = null;
                } else if (contextType == Context.USE_ONLY) {
                    classDefs = determineModuleDefs(offset,line,document,scopeName);
                    if (classDefs.isEmpty())
                        classDefs = null;
                } else {
                    classDefs = determineDefsForClass(offset,line,document,scopeName);
                }
                
                if (scopeName != null)
                    computer = new FortranCompletionProposalComputer(defs, scopeName, document, offset,contextType);

                // Include proposals in this order:
                if (classDefs != null && computer != null) {
                    // If we are working on a type look for internal fields only
                    proposals.addAll(computer.proposalsFromTheseDefs(classDefs));
                } else {    
                    // 1. Local variables, functions, etc.
                    if (computer != null) proposals.addAll(computer.proposalsFromDefs());
                
                    // 2. Code templates
                    for (ICompletionProposal proposal : new FortranTemplateCompletionProcessor().computeCompletionProposals(viewer, offset))
                        proposals.add(proposal);

                    // 3. Intrinsic procedures
                    if (computer != null) proposals.addAll(computer.proposalsFromIntrinsics());
                }
            }
            catch (Exception e)
            {
                errorMessage = e.getClass().getName() + " - " + e.getMessage(); //$NON-NLS-1$
            }
        }
        
        return proposals.toArray(new ICompletionProposal[proposals.size()]);
    }
    
    private final Context determineContext(int offset, int line, IDocument document) throws BadLocationException
    {
        Context contextType = Context.GENERIC;
        // Get line to analyze
        int line_offset = document.getLineOffset(line);
        int cur_length = offset-line_offset;
        String current_line = document.get(line_offset, cur_length);
        current_line = current_line.toLowerCase();
        // Check for beginning keyword
        String keywordList = "[ ]*(class|type|use|allocate|deallocate|nullify)";  //$NON-NLS-1$
        Pattern var_pattern = Pattern.compile(keywordList);
        Matcher matchedKeyword = var_pattern.matcher(current_line);
        String keyword = null;
        if (matchedKeyword.find())
            keyword = matchedKeyword.group();
        if (keyword != null) {
            keyword = keyword.replaceAll(" ",""); //$NON-NLS-1$ //$NON-NLS-2$
            // Determine type of statement
            if (keyword.matches("class|type")) { //$NON-NLS-1$ 
                if (current_line.contains("(") && !current_line.contains(")")) //$NON-NLS-1$ //$NON-NLS-2$
                    contextType=Context.TYPE_VARIABLE_DEF;
            } else if (keyword.matches("allocate")) { //$NON-NLS-1$ 
                if (current_line.contains("(") && !current_line.contains(")")) //$NON-NLS-1$ //$NON-NLS-2$
                    contextType=Context.ALLOCATE;
            } else if (keyword.matches("deallocate|nullify")) { //$NON-NLS-1$ 
                if (current_line.contains("(") && !current_line.contains(")")) //$NON-NLS-1$ //$NON-NLS-2$
                    contextType=Context.DEALLOCATE;
            } else if (keyword.matches("use")) { //$NON-NLS-1$ 
                if (!current_line.contains(":")) //$NON-NLS-1$ 
                    contextType=Context.USE;
                else
                    contextType=Context.USE_ONLY;
            }
        }
        // Unknown context
        return contextType;
    }
    
    private final List<Definition> determineDefsForClass(int offset, int line, IDocument document, String scopeName) throws BadLocationException
    {
        String scopeTemp = scopeName;
        ScopingNode parentScope = null;
        List<Definition> classDefs = null;
        ScopingNode classScope = null;
        // Get line to analyze
        int line_offset = document.getLineOffset(line);
        int cur_length = offset-line_offset;
        String current_line = document.get(line_offset, cur_length);
        String prevChar = document.get(offset-1,1);
        // Compute base variable for current class chain
        String current_variable = null;
        Pattern var_pattern = Pattern.compile("[a-zA-Z0-9_%(,)]*"); //$NON-NLS-1$
        Matcher matched_vars = var_pattern.matcher(current_line);
        while (matched_vars.find()) {
            String var_temp = matched_vars.group();
            if (!var_temp.equals("")) //$NON-NLS-1$
                current_variable = matched_vars.group();
        }
        if (current_variable == null)
            return classDefs;
        if (!(current_variable.contains("%") && current_variable.endsWith(prevChar))) //$NON-NLS-1$
            return classDefs;
        // Handle arrays usage
        current_variable = current_variable.replaceAll("\\(([^\\)]+)\\)", ""); //$NON-NLS-1$ //$NON-NLS-2$
        // Remove leading characters if setting an array index
        int parenLoc = current_variable.lastIndexOf('(');
        if (parenLoc>=0)
        {
            current_variable = current_variable.substring(parenLoc+1);
        }
        // Handle nested classes
        String[] sub_fields = current_variable.split("%"); //$NON-NLS-1$
        String base_variable = sub_fields[0].toLowerCase();
        // Search for base variable in currently available scopes
        String type_name = null;
        Iterable<Definition> proposalsToConsider = null;
        while (true)
        {   
            proposalsToConsider = defs.get(scopeTemp);
            if (proposalsToConsider != null)
            {
                for (Definition def : proposalsToConsider)
                {
                    if (!def.isLocalVariable())
                        continue;
                    String identifier = def.getCanonicalizedName();
                    if (!identifier.equals(base_variable))
                        continue;
                    // Base variable definition found
                    Type var_type = def.getType();
                    if (var_type instanceof DerivedType ) {
                        DerivedType typeNode = (DerivedType) var_type;
                        type_name = typeNode.getName();
                        break;
                    }
                }
            }
            // Exit if type name was determined
            if (type_name != null)
                break;
            // Step to next outer scope if declaration has not been found
            int colon = scopeTemp.indexOf(':');
            if (colon < 0)
                break;
            else
                scopeTemp = scopeTemp.substring(colon+1);
        }
        // Step up remaining scopes looking for type definition
        outerloop:
            while (true)
            {   
                proposalsToConsider = defs.get(scopeTemp);
                if (proposalsToConsider != null)
                {
                    for (Definition def : proposalsToConsider)
                    {
                        if (!def.isDerivedType())
                            continue;
                        String identifier = def.getCanonicalizedName();
                        // Type definition found
                        if (identifier.equals(type_name)) {
                            PhotranTokenRef mytoken = def.getTokenRef();
                            Token mydef = mytoken.getASTNode();
                            parentScope = mydef.getEnclosingScope();
                            classScope = mydef.getLocalScope();
                            // Get known definitions
                            classDefs = classScope.getAllDefinitions();
                            // If class chain wait till top level
                            if (sub_fields.length > 1) {
                                break;
                            } else {
                                break outerloop;
                            }
                        }
                    }
                }
                //
                if (classDefs != null)
                    break;
                // Step to next outer scope if declaration has not been found
                int colon = scopeTemp.indexOf(':');
                if (colon < 0)
                    break;
                else
                    scopeTemp = scopeTemp.substring(colon+1);
            }
        // Step up class chain to current type
        if (sub_fields.length > 1) {
            for (int i = 1; i<sub_fields.length; i=i+1)
            {
                if (sub_fields[i]=="") //$NON-NLS-1$
                    break;
                // Find variable in current type
                proposalsToConsider = classDefs;
                if (proposalsToConsider != null)
                {
                    for (Definition def : proposalsToConsider)
                    {
                        if (!def.isDerivedTypeComponent())
                            continue;
                        //
                        String identifier = def.getCanonicalizedName();
                        if (!identifier.equals(sub_fields[i].toLowerCase()))
                            continue;
                        // Get type name
                        Type var_type = def.getType();
                        if (var_type instanceof DerivedType ) {
                            DerivedType typeNode = (DerivedType) var_type;
                            type_name = typeNode.getName();
                            break;
                        }
                    }
                }
                // Find new type in parent scope
                proposalsToConsider = parentScope.getAllDefinitions();
                if (proposalsToConsider != null)
                {
                    for (Definition def : proposalsToConsider)
                    {
                        if (!def.isDerivedType())
                            continue;
                        //
                        String identifier = def.getCanonicalizedName();
                        if (!identifier.equals(type_name))
                            continue;
                        //
                        PhotranTokenRef mytoken = def.getTokenRef();
                        // Look in parent scope
                        Token mydef = mytoken.getASTNode();
                        List<PhotranTokenRef> possParents = parentScope.manuallyResolve(mydef);
                        mytoken = possParents.get(0);
                        mydef = mytoken.getASTNode();
                        classScope = mydef.getLocalScope();
                        parentScope = mydef.getEnclosingScope();
                        // Get known definitions
                        classDefs = classScope.getAllDefinitions();
                        break;
                    }
                }
            }
        }
        // If we have located the scope get definitions
        if (classScope != null) {
            // Definitions for this scope
            classDefs = classScope.getAllDefinitions();
            // Process inherited elements
            PhotranTokenRef mytoken = null;
            while (true) {
                if (classScope instanceof ASTDerivedTypeDefNode ) {
                    Token parentClass = null;
                    ASTDerivedTypeDefNode typeNode = (ASTDerivedTypeDefNode) classScope;
                    // Look for inheritance node
                    ASTDerivedTypeStmtNode typeDef =  typeNode.getDerivedTypeStmt();
                    IASTListNode<ASTTypeAttrSpecNode> defList = typeDef.getTypeAttrSpecList();
                    if (defList != null) {
                        for (ASTTypeAttrSpecNode currDef: defList) {
                            if (!currDef.isExtends())
                                continue;
                            parentClass = currDef.getParentTypeName();
                            List<PhotranTokenRef> possParents = parentScope.manuallyResolve(parentClass);
                            mytoken = possParents.get(0);
                            parentClass = mytoken.getASTNode();
                        }
                    }
                    // Scan parent class for new methods/variables
                    if (parentClass==null) {
                        break;
                    } else {
                        ScopingNode tempScope = parentClass.getLocalScope();
                        // Get known definitions
                        List<Definition> tempDefs = tempScope.getAllDefinitions();
                        for (Definition newDef: tempDefs) {
                            boolean overridden = false;
                            for (Definition currDef: classDefs){
                                String currIden = currDef.getCanonicalizedName();
                                if (currIden.equals(newDef.getCanonicalizedName()))
                                    overridden = true;
                            }
                            if (!overridden)
                                classDefs.add(newDef);
                        }
                        // Update scope to move up inheritance ladder
                        classScope = tempScope;
                    }
                }
            }
        }
        return classDefs;
    }
    
    private final List<Definition> determineModuleNames()
    {
        List<Definition> moduleDefs = new LinkedList<Definition>();
        PhotranVPG vpg = PhotranVPG.getInstance();
        Iterable<String> moduleNames = vpg.listAllModules();
        for (String module: moduleNames) {
            Definition dummyDef = new Definition(module,null,Classification.MODULE,Type.VOID);
            moduleDefs.add(dummyDef);
        }
        return moduleDefs;
    }
    
    private final List<Definition> determineModuleDefs(int offset, int line, IDocument document, String scopeName) throws BadLocationException
    {
        List<Definition> moduleDefs = new LinkedList<Definition>();
        // Get line to analyze
        int line_offset = document.getLineOffset(line);
        int cur_length = offset-line_offset;
        String current_line = document.get(line_offset, cur_length);
        current_line = current_line.toLowerCase();
        // Check for beginning keyword
        String varChars = "[a-z0-9_]*";  //$NON-NLS-1$
        Pattern var_pattern = Pattern.compile(varChars);
        Matcher matchedKeyword = var_pattern.matcher(current_line);
        String moduleName = null;
        while (matchedKeyword.find()) {
            moduleName = matchedKeyword.group();
            if (!(moduleName.matches("use") || moduleName.isEmpty()))  //$NON-NLS-1$
                break;
        }
        if (moduleName == null)
            return moduleDefs;
        // Find module and get defs
        PhotranVPG vpg = PhotranVPG.getInstance();
        ArrayList<Definition> modules = vpg.findAllModulesNamed(moduleName);
        if (modules != null) {
            if ((modules.isEmpty() || modules.size()>1))
                return moduleDefs;
            //
            Definition module = modules.get(0);
            PhotranTokenRef moduleTokenRef = module.getTokenRef();
            Token moduleToken = moduleTokenRef.getASTNode();
            ScopingNode moduleScope = moduleToken.getLocalScope();
            List<Definition> tempDefs = moduleScope.getAllPublicDefinitions();
            String moduleFile = moduleTokenRef.getFilename();
            for (Definition def: tempDefs) {
                PhotranTokenRef defTokenRef = def.getTokenRef();
                if (moduleFile.equals(defTokenRef.getFilename()))
                    moduleDefs.add(def);
            }
        }
        return moduleDefs;
    }

    private int determineLineNumberForOffset(int offset, IDocument document) throws BadLocationException
    {
        int line = document.getLineOfOffset(offset);
        
        /*
         * The mapping between scopes and lines is reconfigured only when
         * the editor is reconciled and a new AST is available.  Therefore,
         * if the user adds a line or two at the bottom of the file and
         * invokes content assist before the editor can be reconciled, the
         * line number may be greater than what the last line of the file
         * was the last time we parsed it.  In this case, reset the line
         * to be the last line of the file.
         */
        if (!scopes.isEmpty() && line >= scopes.size())
            line = scopes.size()-1;
        return line;
    }

    private final String determineScopeNameForLine(int line)
    {
        String scopeName = ""; //$NON-NLS-1$
        if (line < scopes.size())
            scopeName = scopes.get(line);
        if (scopeName == null)
            scopeName = ""; //$NON-NLS-1$
        
        /*
         * Again, the mapping between scopes and lines is reconfigured only
         * when the editor is reconciled and a new AST is available.  If
         * the user invokes content assist and the scope is reported to be
         * the outermost scope (""), it's probably because the user has
         * added lines beyond the end-statement of the previous program
         * unit and the editor hasn't been reconciled yet.  (The outermost
         * scope usually contains just a single module or program, so
         * it is rare that a user would type anything below the
         * first program unit, particularly in the outermost scope).
         * Therefore, populate the list of completion proposals based on
         * the *preceding* scope.
         */
        while (scopeName.equals("") && line > 0) //$NON-NLS-1$
        {
            line--;
            if (line < scopes.size())
                scopeName = scopes.get(line);
        }
        return scopeName;
    }

    public IContextInformation[] computeContextInformation(ITextViewer viewer,
                                                           int offset)
    {
        return null;
    }

    public char[] getCompletionProposalAutoActivationCharacters()
    {
        return null;
    }

    public char[] getContextInformationAutoActivationCharacters()
    {
        return null;
    }

    public IContextInformationValidator getContextInformationValidator()
    {
        return null;
    }

    public String getErrorMessage()
    {
        return errorMessage;
    }
}
