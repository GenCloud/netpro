/*
 * Copyright 2011-2015 L2EMU UNIQUE
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.l2emuproject.proxy.io.exception;

import java.io.IOException;
import java.util.Iterator;
import java.util.zip.DataFormatException;

/**
 * Allows an {@link Iterator} to throw {@link IOException}s.
 * 
 * @author _dev_
 */
public final class LogFileIterationIOException extends RuntimeException
{
	private static final long serialVersionUID = -6078123348876945064L;
	
	/**
	 * Wraps an {@link IOException}.
	 * 
	 * @param filename associated log file
	 * @param cause the real cause
	 */
	public LogFileIterationIOException(String filename, IOException cause)
	{
		super(cause);
	}
	
	/**
	 * Wraps a {@link DataFormatException}.
	 * 
	 * @param filename associated log file
	 * @param cause the real cause
	 */
	public LogFileIterationIOException(String filename, DataFormatException cause)
	{
		super(cause);
	}
}
