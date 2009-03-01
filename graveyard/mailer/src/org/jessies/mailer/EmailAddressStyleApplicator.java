package org.jessies.mailer;

import e.ptextarea.*;
import java.util.regex.*;

public class EmailAddressStyleApplicator extends RegularExpressionStyleApplicator {
    public EmailAddressStyleApplicator(PTextArea textArea) {
        super(textArea, "\\b([A-Za-z][A-Za-z0-9-_.]+@[A-Za-z0-9-.]+)\\b", PStyle.HYPERLINK);
    }
    
    @Override
    public boolean canApplyStylingTo(PStyle style) {
        return (style == PStyle.NORMAL || style == PStyle.COMMENT);
    }
    
    @Override
    protected void configureSegment(PTextSegment segment, Matcher matcher) {
        // FIXME: since we're supposed to be the mailer around here, we should probably talk directly to ourselves.
        // FIXME: long term, though, we probably want to make sure we're handling all the native system's mailto:s.
        //String url = matcher.group(1);
        //segment.setLinkAction(new WebLinkAction("Web Link", url));
    }
}
