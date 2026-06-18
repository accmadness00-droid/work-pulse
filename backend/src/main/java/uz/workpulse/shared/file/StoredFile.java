package uz.workpulse.shared.file;

public class StoredFile {
    private final String fileName;
    private final String path;
    private final String url;

    public StoredFile(String fileName, String path, String url) {
        this.fileName = fileName;
        this.path = path;
        this.url = url;
    }

    public String getFileName() { return fileName; }
    public String getPath() { return path; }
    public String getUrl() { return url; }
}
