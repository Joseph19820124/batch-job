using System.IO;
using Google.Apis.Auth.OAuth2;
using Google.Apis.Auth.OAuth2.Flows;
using Google.Apis.Auth.OAuth2.Responses;
using Google.Apis.Drive.v3;
using Google.Apis.Services;
using Google.Apis.Upload;
using DriveFile = Google.Apis.Drive.v3.Data.File;

namespace DriveUploader;

public class GoogleDriveService
{
    private readonly DriveService _driveService;
    private readonly Dictionary<string, string> _folderCache = new();

    public GoogleDriveService(GoogleDriveSettings settings)
    {
        var flow = new GoogleAuthorizationCodeFlow(new GoogleAuthorizationCodeFlow.Initializer
        {
            ClientSecrets = new ClientSecrets
            {
                ClientId = settings.ClientId,
                ClientSecret = settings.ClientSecret
            },
            Scopes = [DriveService.Scope.DriveFile]
        });

        var token = new TokenResponse { RefreshToken = settings.RefreshToken };
        var credential = new UserCredential(flow, "user", token);

        _driveService = new DriveService(new BaseClientService.Initializer
        {
            HttpClientInitializer = credential,
            ApplicationName = "DriveUploader"
        });
    }

    public async Task<string> CreateFolderAsync(string folderName, string parentId, CancellationToken ct)
    {
        var cacheKey = $"{parentId}/{folderName}";
        if (_folderCache.TryGetValue(cacheKey, out var cachedId))
        {
            return cachedId;
        }

        var existingFolder = await FindFolderAsync(folderName, parentId, ct);
        if (existingFolder != null)
        {
            _folderCache[cacheKey] = existingFolder.Id;
            return existingFolder.Id;
        }

        var folderMetadata = new DriveFile
        {
            Name = folderName,
            MimeType = "application/vnd.google-apps.folder",
            Parents = [parentId]
        };

        var request = _driveService.Files.Create(folderMetadata);
        request.SupportsAllDrives = true;
        request.Fields = "id";

        var folder = await request.ExecuteAsync(ct);
        _folderCache[cacheKey] = folder.Id;
        return folder.Id;
    }

    private async Task<DriveFile?> FindFolderAsync(string folderName, string parentId, CancellationToken ct)
    {
        var escapedName = folderName.Replace("'", "\\'");
        var query = $"name = '{escapedName}' and '{parentId}' in parents and mimeType = 'application/vnd.google-apps.folder' and trashed = false";

        var request = _driveService.Files.List();
        request.Q = query;
        request.SupportsAllDrives = true;
        request.IncludeItemsFromAllDrives = true;
        request.Fields = "files(id, name)";

        var result = await request.ExecuteAsync(ct);
        return result.Files.FirstOrDefault();
    }

    public async Task<DriveFile> UploadFileAsync(string filePath, string folderId, CancellationToken ct)
    {
        var fileName = Path.GetFileName(filePath);
        var mimeType = GetMimeType(filePath);

        var fileMetadata = new DriveFile
        {
            Name = fileName,
            Parents = [folderId]
        };

        await using var stream = new FileStream(filePath, FileMode.Open, FileAccess.Read);

        var request = _driveService.Files.Create(fileMetadata, stream, mimeType);
        request.SupportsAllDrives = true;
        request.Fields = "id, name, size, webViewLink";

        var result = await request.UploadAsync(ct);

        if (result.Status == UploadStatus.Failed)
        {
            throw new Exception($"Upload failed: {result.Exception?.Message ?? "Unknown error"}");
        }

        return request.ResponseBody;
    }

    private static string GetMimeType(string filePath)
    {
        var extension = Path.GetExtension(filePath).ToLowerInvariant();
        return extension switch
        {
            ".txt" => "text/plain",
            ".pdf" => "application/pdf",
            ".doc" => "application/msword",
            ".docx" => "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            ".xls" => "application/vnd.ms-excel",
            ".xlsx" => "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            ".ppt" => "application/vnd.ms-powerpoint",
            ".pptx" => "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            ".jpg" or ".jpeg" => "image/jpeg",
            ".png" => "image/png",
            ".gif" => "image/gif",
            ".bmp" => "image/bmp",
            ".svg" => "image/svg+xml",
            ".mp3" => "audio/mpeg",
            ".mp4" => "video/mp4",
            ".avi" => "video/x-msvideo",
            ".mov" => "video/quicktime",
            ".zip" => "application/zip",
            ".rar" => "application/vnd.rar",
            ".7z" => "application/x-7z-compressed",
            ".json" => "application/json",
            ".xml" => "application/xml",
            ".html" or ".htm" => "text/html",
            ".css" => "text/css",
            ".js" => "application/javascript",
            ".csv" => "text/csv",
            _ => "application/octet-stream"
        };
    }
}
