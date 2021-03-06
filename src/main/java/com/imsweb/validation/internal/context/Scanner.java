/*
 * Copyright (C) 2011 Information Management Services, Inc.
 */
package com.imsweb.validation.internal.context;

/**
 * Java context scanner interface.
 * <p/>
 * Created on Oct 7, 2011 by depryf
 * @author depryf
 */
public interface Scanner {

    /**
     * Created on Oct 4, 2011 by murphyr
     * @return the next <code>Symbol</code>
     * @throws Exception
     */
    Symbol next_token() throws Exception;
}
