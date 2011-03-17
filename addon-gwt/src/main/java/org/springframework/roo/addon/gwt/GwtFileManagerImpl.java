package org.springframework.roo.addon.gwt;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.PhysicalTypeMetadataProvider;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileCopyUtils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

/**
 * Provides a basic implementation of {@link GwtFileManager} which encapsulates
 * the file management functionality required by {@link GwtMetadataProviderImpl}
 *
 * @author James Tyrrell
 * @since 1.1.2
 */

@Component
@Service
public class GwtFileManagerImpl implements GwtFileManager {
	private static String ROO_EDIT_WARNING = "// WARNING: DO NOT EDIT THIS FILE. THIS FILE IS MANAGED BY SPRING ROO.\n\n";
	@Reference private FileManager fileManager;
	@Reference private PhysicalTypeMetadataProvider physicalTypeMetadataProvider;
	@Reference private TypeLocationService typeLocationService;

	public void write(String destFile, String newContents) {
		write(destFile, newContents, true);
	}

	private void write(String destFile, String newContents, boolean overwrite) {
		// Write to disk, or update a file if it is already present
		MutableFile mutableFile = null;
		if (fileManager.exists(destFile)) {
			if (!overwrite) {
				return;
			}
			// First verify if the file has even changed
			File f = new File(destFile);
			String existing = null;
			try {
				existing = FileCopyUtils.copyToString(new FileReader(f));
			} catch (IOException ignored) {
			}

			if (!newContents.equals(existing)) {
				mutableFile = fileManager.updateFile(destFile);
			}
		} else {
			mutableFile = fileManager.createFile(destFile);
			Assert.notNull(mutableFile, "Could not create output file '" + destFile + "'");
		}

		if (mutableFile != null) {
			try {
				FileCopyUtils.copy(newContents.getBytes(), mutableFile.getOutputStream());
			} catch (IOException ioe) {
				throw new IllegalStateException("Could not output '" + mutableFile.getCanonicalPath() + "'", ioe);
			}
		}

		// If mutableFile is null, that means the source == destination content
	}

	public void write(ClassOrInterfaceTypeDetails typeDetails) {
		write(typeDetails, false);
	}

	public void write(ClassOrInterfaceTypeDetails typeDetails, boolean includeWarning) {
		String destFile = typeLocationService.getPhysicalLocationCanonicalPath(typeDetails.getDeclaredByMetadataId());
		includeWarning &= !destFile.endsWith(".xml");
		includeWarning |= destFile.endsWith("_Roo_Gwt.java");
		String fileContents = physicalTypeMetadataProvider.getCompilationUnitContents(typeDetails);
		if (includeWarning) {
			fileContents = ROO_EDIT_WARNING + fileContents;
		} else if (fileManager.exists(destFile)) {
			return;
		}
		write(destFile, fileContents, includeWarning);
	}

	public void write(List<ClassOrInterfaceTypeDetails> typeDetails, boolean includeWarning) {
		for (ClassOrInterfaceTypeDetails typeDetail : typeDetails) {
			write(typeDetail, includeWarning);
		}
	}

	public void write(List<ClassOrInterfaceTypeDetails> typeDetails) {
		for (ClassOrInterfaceTypeDetails typeDetail : typeDetails) {
			write(typeDetail);
		}
	}
}
