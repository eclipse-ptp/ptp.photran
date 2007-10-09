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
package org.eclipse.photran.internal.ui.actions;

import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.photran.core.vpg.PhotranTokenRef;
import org.eclipse.photran.core.vpg.PhotranVPG;
import org.eclipse.photran.internal.core.analysis.binding.Definition;
import org.eclipse.photran.internal.core.analysis.binding.ImplicitSpec;
import org.eclipse.photran.internal.core.analysis.binding.ScopingNode;
import org.eclipse.photran.internal.core.parser.ASTExecutableProgramNode;
import org.eclipse.photran.internal.core.parser.GenericParseTreeVisitor;
import org.eclipse.photran.internal.core.parser.Parser.InteriorNode;

/**
 * Implements the Display Symbol Table action in the Refactor/(Debugging) menu
 * 
 * @author Jeff Overbey
 */
public class DisplaySymbolTable extends FortranEditorASTActionDelegate
{
    public void run(IProgressMonitor progressMonitor) throws InvocationTargetException, InterruptedException
    {
        try
        {
        	progressMonitor.beginTask("Waiting for background work to complete (synchronizing Fortran virtual program graph)", IProgressMonitor.UNKNOWN);

            File temp = createTempFile();
            final PrintStream ps = createPrintStream(temp);

            ps.println("SYMBOL TABLE - Derived from Virtual Program Graph");
            
        	getAST().visitUsing(new GenericParseTreeVisitor()
        	{
        		private int indentation = 0;
        		
				@Override public void preparingToVisitChildrenOf(InteriorNode node)
				{
					if (!(node instanceof ScopingNode)) return;
					if (node instanceof ASTExecutableProgramNode && node.getParent() != null) return;
					
					ScopingNode scope = (ScopingNode)node;
					
					ps.println();
					describeScope(scope);
					
					indentation += 4;
					
					try
					{
						for (Definition d : scope.getAllDefinitions())
							println(describeDeclaration(d));
					}
					catch (Exception e)
					{
						println("EXCEPTION: " + e.getClass().getName() + ": " + e.getMessage());
						e.printStackTrace(ps);
					}
				}

				@Override public void doneVisitingChildrenOf(InteriorNode node)
				{
					if (!(node instanceof ScopingNode)) return;
					if (node instanceof ASTExecutableProgramNode && node.getParent() != null) return;
					
					indentation -= 4;
				}

				private void describeScope(ScopingNode scope)
				{
					PhotranTokenRef representativeToken = scope.getRepresentativeToken();
					
					if (representativeToken.getOffset() < 0)
						print("(Global Scope)");
					else
						print("Scope: " + representativeToken.getText());
					
					if (scope.isInternal()) ps.print(" (Internal Subprogram)");
					
					if (scope.isDefaultVisibilityPrivate()) ps.print(" - Default Visibility is PRIVATE");
					
					ImplicitSpec implicitSpec = scope.getImplicitSpec();
					if (implicitSpec == null)
						ps.print(" - Implicit None");
					else
						ps.print(" - " + implicitSpec.toString());
					
					ps.println();
				}
        		
				private void print(String text)
				{
					for (int i = 0; i < indentation; i++)
						ps.print(' ');
					ps.print(text);
				}
				
				private void println(String text)
				{
					print(text);
					ps.println();
				}
        	});
            
            ps.close();
            openHtmlViewerOn("Symbol Table", temp);
        }
        catch (Exception e)
        {
        	String message = e.getMessage();
        	if (message == null) message = e.getClass().getName();
        	MessageDialog.openError(getFortranEditor().getShell(), "Error", message);
        }
        finally
        {
        	progressMonitor.done();
        }
    }

//  ps.println("Symbol Table for " + editor.getIFile().getName());
//  IFortranAST ast = FortranWorkspace.getInstance().acquireTU(file,
//                                           new IncludeLoaderCallback(file.getProject())
//                                           {
//                                              public String onUnableToLoad(String message, String filename)
//                                              {
//                                                  ps.println("ERROR: " + message);
//                                                  return null;
//                                              }
//                                           },
//                                           new ModuleLoaderCallback()
//                                           {
//                                              public String onUnableToLoad(String message, Token moduleName)
//                                              {
//                                                  ps.println("ERROR: " + message);
//                                                  return null;
//                                              }
//                                           },
//                                           editor.isFixedForm(),
//                                           progressMonitor);
//  ps.println();
//  for (Token t : new IterableWrapper<Token>(ast))
//  {
//      Scope scope = (Scope)t.getScope();
//      if (scope != null)
//      {
//          scope.printGlobalSymbolTableOn(ps);
//          break;
//      }
//  }
}