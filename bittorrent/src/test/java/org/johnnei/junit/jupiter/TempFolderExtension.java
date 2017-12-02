package org.johnnei.junit.jupiter;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

public class TempFolderExtension implements BeforeEachCallback, AfterEachCallback, ParameterResolver {

	@Override
	public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
		return parameterContext.getParameter().isAnnotationPresent(Folder.class);
	}

	@Override
	public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
		return getStore(extensionContext).get(extensionContext.getRequiredTestMethod(), Path.class);
	}

	private ExtensionContext.Store getStore(ExtensionContext context) {
		return context.getStore(ExtensionContext.Namespace.create(getClass(), context));
	}

	@Override
	public void afterEach(ExtensionContext context) throws Exception {
		getStore(context).remove(context.getRequiredTestMethod(), Path.class);
	}

	@Override
	public void beforeEach(ExtensionContext context) throws Exception {
		Path tmpDir = Files.createTempDirectory("tmp-junit");
		getStore(context).put(context.getRequiredTestMethod(), tmpDir);
	}
}
