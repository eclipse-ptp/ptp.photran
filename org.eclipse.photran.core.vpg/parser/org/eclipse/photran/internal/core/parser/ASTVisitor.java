/*******************************************************************************
 * Copyright (c) 2007 University of Illinois at Urbana-Champaign and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     UIUC - Initial API and implementation
 *******************************************************************************/
package org.eclipse.photran.internal.core.parser;

import org.eclipse.photran.internal.core.lexer.*;                   import org.eclipse.photran.internal.core.analysis.binding.ScopingNode;

import org.eclipse.photran.internal.core.parser.Parser.*;

public class ASTVisitor
{
    public void visitASTAcImpliedDoNode(ASTAcImpliedDoNode node) {}
    public void visitASTAcValueListNode(ASTAcValueListNode node) {}
    public void visitASTAcValueNode(ASTAcValueNode node) {}
    public void visitASTAccessIdListNode(ASTAccessIdListNode node) {}
    public void visitASTAccessSpecNode(ASTAccessSpecNode node) {}
    public void visitASTAccessStmtNode(ASTAccessStmtNode node) {}
    public void visitASTAllocatableStmtNode(ASTAllocatableStmtNode node) {}
    public void visitASTAllocateObjectListNode(ASTAllocateObjectListNode node) {}
    public void visitASTAllocateObjectNode(ASTAllocateObjectNode node) {}
    public void visitASTAllocateStmtNode(ASTAllocateStmtNode node) {}
    public void visitASTAllocationListNode(ASTAllocationListNode node) {}
    public void visitASTAllocationNode(ASTAllocationNode node) {}
    public void visitASTArithmeticIfStmtNode(ASTArithmeticIfStmtNode node) {}
    public void visitASTArrayAllocationListNode(ASTArrayAllocationListNode node) {}
    public void visitASTArrayAllocationNode(ASTArrayAllocationNode node) {}
    public void visitASTArrayConstructorNode(ASTArrayConstructorNode node) {}
    public void visitASTArrayDeclaratorListNode(ASTArrayDeclaratorListNode node) {}
    public void visitASTArrayDeclaratorNode(ASTArrayDeclaratorNode node) {}
    public void visitASTArrayElementNode(ASTArrayElementNode node) {}
    public void visitASTArraySpecNode(ASTArraySpecNode node) {}
    public void visitASTAssignStmtNode(ASTAssignStmtNode node) {}
    public void visitASTAssignedGotoStmtNode(ASTAssignedGotoStmtNode node) {}
    public void visitASTAssignmentStmtNode(ASTAssignmentStmtNode node) {}
    public void visitASTAssumedShapeSpecListNode(ASTAssumedShapeSpecListNode node) {}
    public void visitASTAssumedSizeSpecNode(ASTAssumedSizeSpecNode node) {}
    public void visitASTAttrSpecNode(ASTAttrSpecNode node) {}
    public void visitASTAttrSpecSeqNode(ASTAttrSpecSeqNode node) {}
    public void visitASTBackspaceStmtNode(ASTBackspaceStmtNode node) {}
    public void visitASTBlockDataBodyNode(ASTBlockDataBodyNode node) {}
    public void visitASTBlockDataNameNode(ASTBlockDataNameNode node) {}
    public void visitASTBlockDataStmtNode(ASTBlockDataStmtNode node) {}
    public void visitASTBlockDataSubprogramNode(ASTBlockDataSubprogramNode node) {}
    public void visitASTBodyNode(ASTBodyNode node) {}
    public void visitASTBozLiteralConstantNode(ASTBozLiteralConstantNode node) {}
    public void visitASTCallStmtNode(ASTCallStmtNode node) {}
    public void visitASTCaseConstructNode(ASTCaseConstructNode node) {}
    public void visitASTCaseStmtNode(ASTCaseStmtNode node) {}
    public void visitASTCaseValueRangeListNode(ASTCaseValueRangeListNode node) {}
    public void visitASTCaseValueRangeNode(ASTCaseValueRangeNode node) {}
    public void visitASTCharLengthNode(ASTCharLengthNode node) {}
    public void visitASTCharSelectorNode(ASTCharSelectorNode node) {}
    public void visitASTCloseSpecListNode(ASTCloseSpecListNode node) {}
    public void visitASTCloseSpecNode(ASTCloseSpecNode node) {}
    public void visitASTCloseStmtNode(ASTCloseStmtNode node) {}
    public void visitASTCommonBlockListNode(ASTCommonBlockListNode node) {}
    public void visitASTCommonBlockNameNode(ASTCommonBlockNameNode node) {}
    public void visitASTCommonBlockNode(ASTCommonBlockNode node) {}
    public void visitASTCommonBlockObjectListNode(ASTCommonBlockObjectListNode node) {}
    public void visitASTCommonBlockObjectNode(ASTCommonBlockObjectNode node) {}
    public void visitASTCommonStmtNode(ASTCommonStmtNode node) {}
    public void visitASTComplexConstNode(ASTComplexConstNode node) {}
    public void visitASTComponentAttrSpecListNode(ASTComponentAttrSpecListNode node) {}
    public void visitASTComponentAttrSpecNode(ASTComponentAttrSpecNode node) {}
    public void visitASTComponentDeclListNode(ASTComponentDeclListNode node) {}
    public void visitASTComponentDeclNode(ASTComponentDeclNode node) {}
    public void visitASTComponentDefStmtNode(ASTComponentDefStmtNode node) {}
    public void visitASTComponentInitializationNode(ASTComponentInitializationNode node) {}
    public void visitASTComponentNameNode(ASTComponentNameNode node) {}
    public void visitASTComputedGotoStmtNode(ASTComputedGotoStmtNode node) {}
    public void visitASTConditionalBodyNode(ASTConditionalBodyNode node) {}
    public void visitASTConnectSpecListNode(ASTConnectSpecListNode node) {}
    public void visitASTConnectSpecNode(ASTConnectSpecNode node) {}
    public void visitASTConstantNode(ASTConstantNode node) {}
    public void visitASTContainsStmtNode(ASTContainsStmtNode node) {}
    public void visitASTContinueStmtNode(ASTContinueStmtNode node) {}
    public void visitASTCycleStmtNode(ASTCycleStmtNode node) {}
    public void visitASTDataIDoObjectListNode(ASTDataIDoObjectListNode node) {}
    public void visitASTDataImpliedDoNode(ASTDataImpliedDoNode node) {}
    public void visitASTDataRefNode(ASTDataRefNode node) {}
    public void visitASTDataStmtNode(ASTDataStmtNode node) {}
    public void visitASTDataStmtObjectListNode(ASTDataStmtObjectListNode node) {}
    public void visitASTDataStmtSetNode(ASTDataStmtSetNode node) {}
    public void visitASTDataStmtValueListNode(ASTDataStmtValueListNode node) {}
    public void visitASTDataStmtValueNode(ASTDataStmtValueNode node) {}
    public void visitASTDatalistNode(ASTDatalistNode node) {}
    public void visitASTDeallocateStmtNode(ASTDeallocateStmtNode node) {}
    public void visitASTDeferredShapeSpecListNode(ASTDeferredShapeSpecListNode node) {}
    public void visitASTDerivedTypeBodyNode(ASTDerivedTypeBodyNode node) {}
    public void visitASTDerivedTypeDefNode(ASTDerivedTypeDefNode node) {}
    public void visitASTDerivedTypeStmtNode(ASTDerivedTypeStmtNode node) {}
    public void visitASTDimensionStmtNode(ASTDimensionStmtNode node) {}
    public void visitASTDoConstructNode(ASTDoConstructNode node) {}
    public void visitASTEditElementNode(ASTEditElementNode node) {}
    public void visitASTElseIfConstructNode(ASTElseIfConstructNode node) {}
    public void visitASTElseIfPartsNode(ASTElseIfPartsNode node) {}
    public void visitASTElseIfStmtNode(ASTElseIfStmtNode node) {}
    public void visitASTElseStmtNode(ASTElseStmtNode node) {}
    public void visitASTElseWhereConstructNode(ASTElseWhereConstructNode node) {}
    public void visitASTElseWhereStmtNode(ASTElseWhereStmtNode node) {}
    public void visitASTEndBlockDataStmtNode(ASTEndBlockDataStmtNode node) {}
    public void visitASTEndDoStmtNode(ASTEndDoStmtNode node) {}
    public void visitASTEndForallStmtNode(ASTEndForallStmtNode node) {}
    public void visitASTEndFunctionStmtNode(ASTEndFunctionStmtNode node) {}
    public void visitASTEndIfStmtNode(ASTEndIfStmtNode node) {}
    public void visitASTEndInterfaceStmtNode(ASTEndInterfaceStmtNode node) {}
    public void visitASTEndModuleStmtNode(ASTEndModuleStmtNode node) {}
    public void visitASTEndNameNode(ASTEndNameNode node) {}
    public void visitASTEndProgramStmtNode(ASTEndProgramStmtNode node) {}
    public void visitASTEndSelectStmtNode(ASTEndSelectStmtNode node) {}
    public void visitASTEndSubroutineStmtNode(ASTEndSubroutineStmtNode node) {}
    public void visitASTEndTypeStmtNode(ASTEndTypeStmtNode node) {}
    public void visitASTEndWhereStmtNode(ASTEndWhereStmtNode node) {}
    public void visitASTEndfileStmtNode(ASTEndfileStmtNode node) {}
    public void visitASTEntityDeclListNode(ASTEntityDeclListNode node) {}
    public void visitASTEntityDeclNode(ASTEntityDeclNode node) {}
    public void visitASTEntryStmtNode(ASTEntryStmtNode node) {}
    public void visitASTEquivalenceObjectListNode(ASTEquivalenceObjectListNode node) {}
    public void visitASTEquivalenceSetListNode(ASTEquivalenceSetListNode node) {}
    public void visitASTEquivalenceSetNode(ASTEquivalenceSetNode node) {}
    public void visitASTEquivalenceStmtNode(ASTEquivalenceStmtNode node) {}
    public void visitASTExecutableProgramNode(ASTExecutableProgramNode node) {}
    public void visitASTExitStmtNode(ASTExitStmtNode node) {}
    public void visitASTExplicitShapeSpecListNode(ASTExplicitShapeSpecListNode node) {}
    public void visitASTExplicitShapeSpecNode(ASTExplicitShapeSpecNode node) {}
    public void visitASTExprListNode(ASTExprListNode node) {}
    public void visitASTExpressionNode(ASTExpressionNode node) {}
    public void visitASTExternalNameListNode(ASTExternalNameListNode node) {}
    public void visitASTExternalStmtNode(ASTExternalStmtNode node) {}
    public void visitASTFieldSelectorNode(ASTFieldSelectorNode node) {}
    public void visitASTFmtSpecNode(ASTFmtSpecNode node) {}
    public void visitASTForallBodyNode(ASTForallBodyNode node) {}
    public void visitASTForallConstructNode(ASTForallConstructNode node) {}
    public void visitASTForallConstructStmtNode(ASTForallConstructStmtNode node) {}
    public void visitASTForallStmtNode(ASTForallStmtNode node) {}
    public void visitASTForallTripletSpecListNode(ASTForallTripletSpecListNode node) {}
    public void visitASTFormatEditNode(ASTFormatEditNode node) {}
    public void visitASTFormatIdentifierNode(ASTFormatIdentifierNode node) {}
    public void visitASTFormatStmtNode(ASTFormatStmtNode node) {}
    public void visitASTFunctionArgListNode(ASTFunctionArgListNode node) {}
    public void visitASTFunctionArgNode(ASTFunctionArgNode node) {}
    public void visitASTFunctionNameNode(ASTFunctionNameNode node) {}
    public void visitASTFunctionParNode(ASTFunctionParNode node) {}
    public void visitASTFunctionParsNode(ASTFunctionParsNode node) {}
    public void visitASTFunctionStmtNode(ASTFunctionStmtNode node) {}
    public void visitASTFunctionSubprogramNode(ASTFunctionSubprogramNode node) {}
    public void visitASTGenericNameNode(ASTGenericNameNode node) {}
    public void visitASTGenericSpecNode(ASTGenericSpecNode node) {}
    public void visitASTGotoStmtNode(ASTGotoStmtNode node) {}
    public void visitASTIfConstructNode(ASTIfConstructNode node) {}
    public void visitASTIfStmtNode(ASTIfStmtNode node) {}
    public void visitASTIfThenStmtNode(ASTIfThenStmtNode node) {}
    public void visitASTImplicitSpecListNode(ASTImplicitSpecListNode node) {}
    public void visitASTImplicitSpecNode(ASTImplicitSpecNode node) {}
    public void visitASTImplicitStmtNode(ASTImplicitStmtNode node) {}
    public void visitASTImpliedDoVariableNode(ASTImpliedDoVariableNode node) {}
    public void visitASTInitializationNode(ASTInitializationNode node) {}
    public void visitASTInputImpliedDoNode(ASTInputImpliedDoNode node) {}
    public void visitASTInputItemListNode(ASTInputItemListNode node) {}
    public void visitASTInquireSpecListNode(ASTInquireSpecListNode node) {}
    public void visitASTInquireSpecNode(ASTInquireSpecNode node) {}
    public void visitASTInquireStmtNode(ASTInquireStmtNode node) {}
    public void visitASTIntentParListNode(ASTIntentParListNode node) {}
    public void visitASTIntentSpecNode(ASTIntentSpecNode node) {}
    public void visitASTIntentStmtNode(ASTIntentStmtNode node) {}
    public void visitASTInterfaceBlockBodyNode(ASTInterfaceBlockBodyNode node) {}
    public void visitASTInterfaceBlockNode(ASTInterfaceBlockNode node) {}
    public void visitASTInterfaceBodyNode(ASTInterfaceBodyNode node) {}
    public void visitASTInterfaceStmtNode(ASTInterfaceStmtNode node) {}
    public void visitASTInternalSubprogramsNode(ASTInternalSubprogramsNode node) {}
    public void visitASTIntrinsicListNode(ASTIntrinsicListNode node) {}
    public void visitASTIntrinsicStmtNode(ASTIntrinsicStmtNode node) {}
    public void visitASTIoControlSpecListNode(ASTIoControlSpecListNode node) {}
    public void visitASTIoControlSpecNode(ASTIoControlSpecNode node) {}
    public void visitASTKindSelectorNode(ASTKindSelectorNode node) {}
    public void visitASTLabelDoStmtNode(ASTLabelDoStmtNode node) {}
    public void visitASTLblRefListNode(ASTLblRefListNode node) {}
    public void visitASTLblRefNode(ASTLblRefNode node) {}
    public void visitASTLogicalConstantNode(ASTLogicalConstantNode node) {}
    public void visitASTLoopControlNode(ASTLoopControlNode node) {}
    public void visitASTMainProgramNode(ASTMainProgramNode node) {}
    public void visitASTMaskExprNode(ASTMaskExprNode node) {}
    public void visitASTMaskedElseWhereConstructNode(ASTMaskedElseWhereConstructNode node) {}
    public void visitASTMaskedElseWhereStmtNode(ASTMaskedElseWhereStmtNode node) {}
    public void visitASTModuleBodyNode(ASTModuleBodyNode node) {}
    public void visitASTModuleNameNode(ASTModuleNameNode node) {}
    public void visitASTModuleNode(ASTModuleNode node) {}
    public void visitASTModuleProcedureStmtNode(ASTModuleProcedureStmtNode node) {}
    public void visitASTModuleStmtNode(ASTModuleStmtNode node) {}
    public void visitASTNameNode(ASTNameNode node) {}
    public void visitASTNamedConstantDefListNode(ASTNamedConstantDefListNode node) {}
    public void visitASTNamedConstantDefNode(ASTNamedConstantDefNode node) {}
    public void visitASTNamedConstantUseNode(ASTNamedConstantUseNode node) {}
    public void visitASTNamelistGroupNameNode(ASTNamelistGroupNameNode node) {}
    public void visitASTNamelistGroupsNode(ASTNamelistGroupsNode node) {}
    public void visitASTNamelistStmtNode(ASTNamelistStmtNode node) {}
    public void visitASTNullifyStmtNode(ASTNullifyStmtNode node) {}
    public void visitASTObjectNameNode(ASTObjectNameNode node) {}
    public void visitASTOnlyListNode(ASTOnlyListNode node) {}
    public void visitASTOnlyNode(ASTOnlyNode node) {}
    public void visitASTOpenStmtNode(ASTOpenStmtNode node) {}
    public void visitASTOperatorNode(ASTOperatorNode node) {}
    public void visitASTOptionalParListNode(ASTOptionalParListNode node) {}
    public void visitASTOptionalStmtNode(ASTOptionalStmtNode node) {}
    public void visitASTOutputImpliedDoNode(ASTOutputImpliedDoNode node) {}
    public void visitASTOutputItemList1Node(ASTOutputItemList1Node node) {}
    public void visitASTOutputItemListNode(ASTOutputItemListNode node) {}
    public void visitASTParameterStmtNode(ASTParameterStmtNode node) {}
    public void visitASTPauseStmtNode(ASTPauseStmtNode node) {}
    public void visitASTPointerFieldNode(ASTPointerFieldNode node) {}
    public void visitASTPointerObjectListNode(ASTPointerObjectListNode node) {}
    public void visitASTPointerObjectNode(ASTPointerObjectNode node) {}
    public void visitASTPointerStmtNode(ASTPointerStmtNode node) {}
    public void visitASTPointerStmtObjectListNode(ASTPointerStmtObjectListNode node) {}
    public void visitASTPointerStmtObjectNode(ASTPointerStmtObjectNode node) {}
    public void visitASTPositionSpecListNode(ASTPositionSpecListNode node) {}
    public void visitASTPositionSpecNode(ASTPositionSpecNode node) {}
    public void visitASTPrefixSpecListNode(ASTPrefixSpecListNode node) {}
    public void visitASTPrefixSpecNode(ASTPrefixSpecNode node) {}
    public void visitASTPrimaryNode(ASTPrimaryNode node) {}
    public void visitASTPrintStmtNode(ASTPrintStmtNode node) {}
    public void visitASTPrivateSequenceStmtNode(ASTPrivateSequenceStmtNode node) {}
    public void visitASTProcedureNameListNode(ASTProcedureNameListNode node) {}
    public void visitASTProgramNameNode(ASTProgramNameNode node) {}
    public void visitASTProgramStmtNode(ASTProgramStmtNode node) {}
    public void visitASTProgramUnitNode(ASTProgramUnitNode node) {}
    public void visitASTRdCtlSpecNode(ASTRdCtlSpecNode node) {}
    public void visitASTRdFmtIdNode(ASTRdFmtIdNode node) {}
    public void visitASTRdIoCtlSpecListNode(ASTRdIoCtlSpecListNode node) {}
    public void visitASTReadStmtNode(ASTReadStmtNode node) {}
    public void visitASTRenameListNode(ASTRenameListNode node) {}
    public void visitASTRenameNode(ASTRenameNode node) {}
    public void visitASTReturnStmtNode(ASTReturnStmtNode node) {}
    public void visitASTRewindStmtNode(ASTRewindStmtNode node) {}
    public void visitASTSFDummyArgNameListNode(ASTSFDummyArgNameListNode node) {}
    public void visitASTSFExprListNode(ASTSFExprListNode node) {}
    public void visitASTSaveStmtNode(ASTSaveStmtNode node) {}
    public void visitASTSavedEntityListNode(ASTSavedEntityListNode node) {}
    public void visitASTSavedEntityNode(ASTSavedEntityNode node) {}
    public void visitASTScalarMaskExprNode(ASTScalarMaskExprNode node) {}
    public void visitASTScalarVariableNode(ASTScalarVariableNode node) {}
    public void visitASTSectionSubscriptListNode(ASTSectionSubscriptListNode node) {}
    public void visitASTSectionSubscriptNode(ASTSectionSubscriptNode node) {}
    public void visitASTSelectCaseBodyNode(ASTSelectCaseBodyNode node) {}
    public void visitASTSelectCaseStmtNode(ASTSelectCaseStmtNode node) {}
    public void visitASTStmtFunctionStmtNode(ASTStmtFunctionStmtNode node) {}
    public void visitASTStopStmtNode(ASTStopStmtNode node) {}
    public void visitASTStructureComponentNode(ASTStructureComponentNode node) {}
    public void visitASTStructureConstructorNode(ASTStructureConstructorNode node) {}
    public void visitASTSubprogramInterfaceBodyNode(ASTSubprogramInterfaceBodyNode node) {}
    public void visitASTSubroutineArgListNode(ASTSubroutineArgListNode node) {}
    public void visitASTSubroutineArgNode(ASTSubroutineArgNode node) {}
    public void visitASTSubroutineNameNode(ASTSubroutineNameNode node) {}
    public void visitASTSubroutineParNode(ASTSubroutineParNode node) {}
    public void visitASTSubroutineParsNode(ASTSubroutineParsNode node) {}
    public void visitASTSubroutineStmtNode(ASTSubroutineStmtNode node) {}
    public void visitASTSubroutineSubprogramNode(ASTSubroutineSubprogramNode node) {}
    public void visitASTSubscriptTripletNode(ASTSubscriptTripletNode node) {}
    public void visitASTSubstringRangeNode(ASTSubstringRangeNode node) {}
    public void visitASTTargetObjectListNode(ASTTargetObjectListNode node) {}
    public void visitASTTargetObjectNode(ASTTargetObjectNode node) {}
    public void visitASTTargetStmtNode(ASTTargetStmtNode node) {}
    public void visitASTTypeDeclarationStmtNode(ASTTypeDeclarationStmtNode node) {}
    public void visitASTTypeNameNode(ASTTypeNameNode node) {}
    public void visitASTTypeSpecNode(ASTTypeSpecNode node) {}
    public void visitASTUnitIdentifierNode(ASTUnitIdentifierNode node) {}
    public void visitASTUnsignedArithmeticConstantNode(ASTUnsignedArithmeticConstantNode node) {}
    public void visitASTUseStmtNode(ASTUseStmtNode node) {}
    public void visitASTVariableNameNode(ASTVariableNameNode node) {}
    public void visitASTVariableNode(ASTVariableNode node) {}
    public void visitASTWhereBodyConstructBlockNode(ASTWhereBodyConstructBlockNode node) {}
    public void visitASTWhereConstructNode(ASTWhereConstructNode node) {}
    public void visitASTWhereConstructStmtNode(ASTWhereConstructStmtNode node) {}
    public void visitASTWhereStmtNode(ASTWhereStmtNode node) {}
    public void visitASTWriteStmtNode(ASTWriteStmtNode node) {}
    public void visitIAccessId(IAccessId node) {}
    public void visitIActionStmt(IActionStmt node) {}
    public void visitIBlockDataBodyConstruct(IBlockDataBodyConstruct node) {}
    public void visitIBodyConstruct(IBodyConstruct node) {}
    public void visitICaseBodyConstruct(ICaseBodyConstruct node) {}
    public void visitIComponentArraySpec(IComponentArraySpec node) {}
    public void visitIDataIDoObject(IDataIDoObject node) {}
    public void visitIDataStmtObject(IDataStmtObject node) {}
    public void visitIDeclarationConstruct(IDeclarationConstruct node) {}
    public void visitIDerivedTypeBodyConstruct(IDerivedTypeBodyConstruct node) {}
    public void visitIExecutableConstruct(IExecutableConstruct node) {}
    public void visitIExecutionPartConstruct(IExecutionPartConstruct node) {}
    public void visitIForallBodyConstruct(IForallBodyConstruct node) {}
    public void visitIInputItem(IInputItem node) {}
    public void visitIInterfaceSpecification(IInterfaceSpecification node) {}
    public void visitIInternalSubprogram(IInternalSubprogram node) {}
    public void visitIModuleBodyConstruct(IModuleBodyConstruct node) {}
    public void visitIModuleSubprogram(IModuleSubprogram node) {}
    public void visitIModuleSubprogramPartConstruct(IModuleSubprogramPartConstruct node) {}
    public void visitIObsoleteActionStmt(IObsoleteActionStmt node) {}
    public void visitIObsoleteExecutionPartConstruct(IObsoleteExecutionPartConstruct node) {}
    public void visitISpecificationPartConstruct(ISpecificationPartConstruct node) {}
    public void visitISpecificationStmt(ISpecificationStmt node) {}
    public void visitIWhereBodyConstruct(IWhereBodyConstruct node) {}
    public void visitToken(Token token) {}
}
