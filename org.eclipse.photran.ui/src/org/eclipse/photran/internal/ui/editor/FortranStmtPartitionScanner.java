package org.eclipse.photran.internal.ui.editor;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.rules.IPartitionTokenScanner;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.photran.internal.core.lang.linescanner.FortranLineScanner;
import org.eclipse.photran.internal.core.lang.linescanner.FortranLineType;
import org.eclipse.photran.internal.core.lang.linescanner.ILookaheadLineReader;
import org.eclipse.photran.internal.ui.FortranUIPlugin;

public class FortranStmtPartitionScanner implements IPartitionTokenScanner
{
    static
    {
        FortranLineType[] lineTypes = FortranLineType.values();

        String[] partitionTypes = new String[lineTypes.length];
        Map<FortranLineType, IToken> tokenTypes = new HashMap<FortranLineType, IToken>();
        for (FortranLineType lineType : lineTypes)
        {
            String partitionType = getPartitionType(lineType);
            partitionTypes[lineType.ordinal()] = partitionType;
            tokenTypes.put(lineType, new Token(partitionType));
        }

        PARTITION_TYPES = partitionTypes;
        LINE_TOKENS = tokenTypes;
    }

    public static final String getPartitionType(FortranLineType lineType)
    {
        switch (lineType)
        {
            case STATEMENT:
                return IDocument.DEFAULT_CONTENT_TYPE;
            default:
                return lineType.toString();
        }
    }

    public static final String[] PARTITION_TYPES;

    private static final Map<FortranLineType, IToken> LINE_TOKENS;

    private final FortranEditor editor;

    private IDocument document = null;

    private int startOffset = 0;

    private int endOffset = 0;

    private int tokenOffset = 0;

    private int tokenLength = 0;

    public FortranStmtPartitionScanner()
    {
        this.editor = null;
    }

    public FortranStmtPartitionScanner(FortranEditor editor)
    {
        this.editor = editor;
    }

    @Override
    public void setRange(IDocument document, int offset, int length)
    {
        this.document = document;
        this.startOffset = offset;
        this.endOffset = offset + length;
    }

    @Override
    public void setPartialRange(IDocument document, int offset, int length, String contentType,
        int partitionOffset)
    {
        this.document = document;
        try
        {
            this.startOffset = document.getPartition(offset).getOffset();
        }
        catch (BadLocationException e)
        {
            FortranUIPlugin.log(e);
            this.startOffset = offset;
        }
        this.endOffset = offset + length;

    }

    @Override
    public IToken nextToken()
    {
        this.tokenOffset = startOffset;
        this.tokenLength = 0;

        if (document == null || endOffset <= startOffset || startOffset >= document.getLength()) { return Token.EOF; }

        try
        {
            final FortranLineScanner lineScanner = new FortranLineScanner(isFixedForm(), isCPreprocessed());
            lineScanner.scan(new DocumentLookaheadLineReader());
            this.tokenLength = lineScanner.getLineLength();
            this.startOffset += this.tokenLength;
            //System.out.printf("Partition: offset %d length %d %s\n[[[%s]]]\n", tokenOffset, tokenLength, lineScanner.getLineType(), document.get(this.tokenOffset, this.tokenLength)); //$NON-NLS-1$
            return LINE_TOKENS.get(lineScanner.getLineType());
        }
        catch (BadLocationException e)
        {
            FortranUIPlugin.log(e);
            return Token.EOF;
        }
    }

    private boolean isFixedForm()
    {
        return editor == null ? false : editor.isFixedForm();
    }

    private boolean isCPreprocessed()
    {
        return editor == null ? true : editor.isCPreprocessed();
    }

    private final class DocumentLookaheadLineReader implements ILookaheadLineReader<BadLocationException>
    {
        private int offset = startOffset;

        public String readNextLine() throws BadLocationException
        {
            if (offset >= document.getLength()) return null;

            int lineNumber = document.getLineOfOffset(offset);
            IRegion line = document.getLineInformation(lineNumber);
            String delimiter = document.getLineDelimiter(lineNumber);
            if (delimiter == null) delimiter = ""; //$NON-NLS-1$
            String result = document.get(line.getOffset(), line.getLength()) + delimiter;
            offset += result.length();
            return result;
        }

        public String advanceAndRestart(int numChars)
        {
            offset = startOffset + numChars; // Pointless, actually, since this is never reused
            return null; // Return value (passed back via FortranLineScanner#scan) not used above
        }

        public void close()
        {
        }
    }

    @Override
    public int getTokenOffset()
    {
        return tokenOffset;
    }

    @Override
    public int getTokenLength()
    {
        return tokenLength;
    }
}
