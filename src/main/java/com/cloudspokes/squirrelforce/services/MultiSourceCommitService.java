package com.cloudspokes.squirrelforce.services;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.egit.github.core.Blob;
import org.eclipse.egit.github.core.Commit;
import org.eclipse.egit.github.core.CommitUser;
import org.eclipse.egit.github.core.Reference;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.Tree;
import org.eclipse.egit.github.core.TreeEntry;
import org.eclipse.egit.github.core.TypedResource;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.DataService;
import org.eclipse.egit.github.core.service.RepositoryService;

/**
 * A service which does a GitHub commit from multiple sources, for example
 * multiple directories. This class has no framework dependencies.
 */
public class MultiSourceCommitService {

    private static final String DEFAULT_COMMIT_MSG = "Auto committed courtesy of Thurgood.";

    private static final String FILE_BLOB_TREE_ENTRY_TYPE = "blob";
    private static final String FILE_BLOB_TREE_ENTRY_MODE = "100644";
    private static final String JAR_FILE_EXT = ".jar";
    private static final String[] DEFAULT_IGNORE_LIST = {"__MACOSX", ".DS_Store", ".git"};

    private final GitHubClient client;
    private final RepositoryService repositoryService;
    private final DataService dataService;
    private final Set<String> binaryFileExtensionsLowercase;
    private final Set<String> entriesToIgnoreLowercase;
    private String organization;
    private String repoOwner;

    private String commitMessage = DEFAULT_COMMIT_MSG;

    public MultiSourceCommitService() {
        this.client = new GitHubClient();

        this.repositoryService = new RepositoryService(this.client);
        this.dataService = new DataService(this.client);

        this.binaryFileExtensionsLowercase = new HashSet<String>();
        setBinaryFileExtensions(JAR_FILE_EXT);

        this.entriesToIgnoreLowercase = new HashSet<String>();
        setEntriesToIgnore(DEFAULT_IGNORE_LIST);
    }

    public MultiSourceCommitService setOrganization(String organization) {
        this.organization = organization;
        return this;
    }

    public MultiSourceCommitService setCredentials(String user, String password, String owner) {
        this.client.setCredentials(user, password);
        this.repoOwner = owner;
        return this;
    }

    public MultiSourceCommitService setOAuth2Token(String accessToken) {
        this.client.setOAuth2Token(accessToken);
        return this;
    }

    /**
     * Specify which file extensions indicate binary files, which will be
     * encoded differently than text files. By default, only .jar files are
     * treated as binary.
     */
    public MultiSourceCommitService setBinaryFileExtensions(String... binaryFileExts) {
        for (String binaryFileExt : binaryFileExts) {
            binaryFileExt = binaryFileExt.trim();
            if (!binaryFileExt.startsWith(".")) {
                binaryFileExt = "." + binaryFileExt;
            }
            this.binaryFileExtensionsLowercase.add(binaryFileExt.toLowerCase());
        }
        return this;
    }

    /**
     * Specify which folder entries to ignore by name. By default, only MACOSX
     * system folders and files are ignored, along with the .git folder.
     */
    public MultiSourceCommitService setEntriesToIgnore(String... entriesToIgnore) {
        for (String entryToIgnore : entriesToIgnore) {
            this.entriesToIgnoreLowercase.add(entryToIgnore.trim().toLowerCase());
        }
        return this;
    }

    /**
     * Specify the commit message to use. Otherwise a default message is used.
     */
    public MultiSourceCommitService setCommitMessage(String commitMessage) {
        this.commitMessage = commitMessage;
        return this;
    }

    /**
     * @param folders folders whose contents will be saved in a new repository
     * @param repoName the name of the new repository
     * @param isPrivate whether the new repository will be private
     * @return htmlUrl of the newly created repository
     * @throws IOException
     */
    public String commitFromFoldersToNewRepo(List<File> folders, String repoName, boolean isPrivate) throws IOException {
        Repository repo = getRepo(repoName);
        List<TreeEntry> treeEntries = new ArrayList<TreeEntry>();

        for (File folder : folders) {
            loadFilesIntoTreeEntries(folder, treeEntries, repo, folder.getPath());
        }

        Tree tree = this.dataService.createTree(repo, treeEntries);
        commit(repo, tree);
        return repo.getHtmlUrl();
    }

    private void loadFilesIntoTreeEntries(File folder, List<TreeEntry> treeEntries, Repository repo, String rootPath) throws IOException {
        String[] folderEntries = folder.list();
        for (String folderEntryName : folderEntries) {
            if (ignore(folderEntryName)) {
                continue;
            }
            File folderEntry = new File(folder, folderEntryName);

            if (folderEntry.isDirectory()) {
                loadFilesIntoTreeEntries(folderEntry, treeEntries, repo, rootPath);

            } else {
                // exclude root path to get relative path
                String relativePath = folderEntry.getPath().substring(rootPath.length() + 1);
                relativePath = relativePath.replace('\\', '/'); // use forward slashes only

                TreeEntry treeEntry;

                if (isBinaryFile(folderEntry)) {
                    String base64 = IOUtils.readContentAsBase64(folderEntry);
                    treeEntry = newBinaryTreeEntry(repo, relativePath, base64);

                } else { // is text file
                    String text = IOUtils.readContentAsText(folderEntry);
                    treeEntry = newTextTreeEntry(repo, relativePath, text);
                }

                treeEntries.add(treeEntry);
            }
        }
    }

    private boolean ignore(String folderEntryName) {
        for (String entryToIgnoreLowercase : this.entriesToIgnoreLowercase) {
            if (folderEntryName.toLowerCase().endsWith(entryToIgnoreLowercase)) {
                return true;
            }
        }
        return false;
    }

    private boolean isBinaryFile(File file) {
        for (String binaryFileExtensionLowercase : this.binaryFileExtensionsLowercase) {
            if (file.getName().toLowerCase().endsWith(binaryFileExtensionLowercase)) {
                return true;
            }
        }
        return false;
    }

    private Repository getRepo(String repoName) throws IOException {
      Repository repo = repositoryService.getRepository(repoOwner, repoName);
      return repo;
  }

//    private Repository newRepo(String repoName, boolean isPrivate) throws IOException {
//        Repository repo = new InitializableRepository();
//        repo.setName(repoName);
//        repo.setPrivate(isPrivate);
//        if (this.organization != null) {
//            repo = this.repositoryService.createRepository(this.organization, repo);
//        } else {
//            repo = this.repositoryService.createRepository(repo);
//        }
//        return repo;
//    }

    private TreeEntry newTextTreeEntry(Repository repo, String path, String content) throws IOException {
        return newTreeEntry(repo, path, content, Blob.ENCODING_UTF8);
    }

    private TreeEntry newBinaryTreeEntry(Repository repo, String path, String content) throws IOException {
        return newTreeEntry(repo, path, content, Blob.ENCODING_BASE64);
    }

    private TreeEntry newTreeEntry(Repository repo, String path, String content, String encoding) throws IOException {
        String blobSha = newBlob(repo, content, encoding);
        TreeEntry entry = new TreeEntry();
        entry.setPath(path);
        entry.setSha(blobSha);
        entry.setType(FILE_BLOB_TREE_ENTRY_TYPE);
        entry.setMode(FILE_BLOB_TREE_ENTRY_MODE);
        return entry;
    }

    private String newBlob(Repository repo, String content, String encoding) throws IOException {
        Blob blob = new Blob();
        blob.setContent(content);
        blob.setEncoding(encoding);
        String sha = this.dataService.createBlob(repo, blob);
        return sha;
    }

    private void commit(Repository repo, Tree tree) throws IOException {
        List<Reference> refs = this.dataService.getReferences(repo);
        Reference masterRef = refs.get(0);
        TypedResource previousCommit = masterRef.getObject();
        String previousCommitSha = previousCommit.getSha();

        Commit newCommit = new Commit();
        newCommit.setMessage(commitMessage);
        newCommit.setTree(tree);
        newCommit.setParents(Arrays.asList(new Commit().setSha(previousCommitSha)));
        CommitUser author = new CommitUser();
        author.setName("Jeff Douglas");
        author.setEmail("jeff@appirio.com");
        author.setDate(new Date());
        newCommit.setAuthor(author);
        newCommit.setCommitter(author);
        newCommit = this.dataService.createCommit(repo, newCommit);

        masterRef.getObject().setSha(newCommit.getSha());
        this.dataService.editReference(repo, masterRef);
    }

}
