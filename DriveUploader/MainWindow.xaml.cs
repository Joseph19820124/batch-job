using System.IO;
using System.Windows;
using System.Windows.Media;
using Microsoft.Extensions.Configuration;

namespace DriveUploader;

public partial class MainWindow : Window
{
    private readonly GoogleDriveService _driveService;
    private CancellationTokenSource? _cts;
    private readonly SolidColorBrush _normalBorder = new(Color.FromRgb(66, 133, 244));
    private readonly SolidColorBrush _dragOverBorder = new(Color.FromRgb(52, 168, 83));

    public MainWindow()
    {
        InitializeComponent();

        var config = new ConfigurationBuilder()
            .SetBasePath(AppDomain.CurrentDomain.BaseDirectory)
            .AddJsonFile("appsettings.json", optional: false)
            .Build();

        var settings = config.GetSection("GoogleDrive").Get<GoogleDriveSettings>()
            ?? throw new InvalidOperationException("GoogleDrive settings not found in appsettings.json");

        _driveService = new GoogleDriveService(settings);
        FolderIdTextBox.Text = settings.FolderId;
    }

    private void DropZone_DragEnter(object sender, DragEventArgs e)
    {
        if (e.Data.GetDataPresent(DataFormats.FileDrop))
        {
            DropZone.BorderBrush = _dragOverBorder;
            DropZone.Background = new SolidColorBrush(Color.FromRgb(232, 245, 233));
            e.Effects = DragDropEffects.Copy;
        }
        else
        {
            e.Effects = DragDropEffects.None;
        }
        e.Handled = true;
    }

    private void DropZone_DragLeave(object sender, DragEventArgs e)
    {
        DropZone.BorderBrush = _normalBorder;
        DropZone.Background = new SolidColorBrush(Color.FromRgb(248, 249, 250));
    }

    private async void DropZone_Drop(object sender, DragEventArgs e)
    {
        DropZone_DragLeave(sender, e);

        if (!e.Data.GetDataPresent(DataFormats.FileDrop))
            return;

        var paths = (string[])e.Data.GetData(DataFormats.FileDrop);
        if (paths.Length == 0)
            return;

        var targetFolderId = FolderIdTextBox.Text.Trim();
        if (string.IsNullOrEmpty(targetFolderId))
        {
            MessageBox.Show("Please enter a target folder ID.", "Error", MessageBoxButton.OK, MessageBoxImage.Warning);
            return;
        }

        var preserveStructure = PreserveStructureCheckBox.IsChecked == true;

        _cts = new CancellationTokenSource();
        CancelButton.IsEnabled = true;
        DropHint.Visibility = Visibility.Collapsed;
        UploadProgress.Visibility = Visibility.Visible;
        LogList.Items.Clear();

        try
        {
            foreach (var path in paths)
            {
                if (Directory.Exists(path))
                {
                    await UploadDirectoryAsync(path, targetFolderId, preserveStructure, _cts.Token);
                }
                else if (File.Exists(path))
                {
                    await UploadSingleFileAsync(path, targetFolderId, _cts.Token);
                }
            }

            StatusText.Text = "Upload complete!";
            Log("All uploads completed successfully.");
        }
        catch (OperationCanceledException)
        {
            StatusText.Text = "Upload cancelled";
            Log("Upload cancelled by user.");
        }
        catch (Exception ex)
        {
            StatusText.Text = "Upload failed";
            Log($"Error: {ex.Message}");
            MessageBox.Show($"Upload failed: {ex.Message}", "Error", MessageBoxButton.OK, MessageBoxImage.Error);
        }
        finally
        {
            CancelButton.IsEnabled = false;
            _cts = null;
        }
    }

    private async Task UploadDirectoryAsync(string directoryPath, string parentFolderId, bool preserveStructure, CancellationToken ct)
    {
        var dirName = Path.GetFileName(directoryPath);
        Log($"Processing directory: {dirName}");

        string targetFolderId;
        if (preserveStructure)
        {
            StatusText.Text = $"Creating folder: {dirName}";
            targetFolderId = await _driveService.CreateFolderAsync(dirName, parentFolderId, ct);
            Log($"Created folder: {dirName}");
        }
        else
        {
            targetFolderId = parentFolderId;
        }

        var files = Directory.GetFiles(directoryPath);
        var subdirs = Directory.GetDirectories(directoryPath);
        var totalItems = files.Length + subdirs.Length;
        var processedItems = 0;

        foreach (var file in files)
        {
            ct.ThrowIfCancellationRequested();
            await UploadSingleFileAsync(file, targetFolderId, ct);
            processedItems++;
            UpdateProgress(processedItems, totalItems);
        }

        foreach (var subdir in subdirs)
        {
            ct.ThrowIfCancellationRequested();
            await UploadDirectoryAsync(subdir, targetFolderId, preserveStructure, ct);
            processedItems++;
            UpdateProgress(processedItems, totalItems);
        }
    }

    private async Task UploadSingleFileAsync(string filePath, string folderId, CancellationToken ct)
    {
        var fileName = Path.GetFileName(filePath);
        StatusText.Text = $"Uploading: {fileName}";
        Log($"Uploading: {fileName}");

        await _driveService.UploadFileAsync(filePath, folderId, ct);
        Log($"Uploaded: {fileName}");
    }

    private void UpdateProgress(int current, int total)
    {
        ProgressBar.Value = total > 0 ? (double)current / total * 100 : 0;
        ProgressText.Text = $"{current} / {total} items";
    }

    private void Log(string message)
    {
        var timestamp = DateTime.Now.ToString("HH:mm:ss");
        LogList.Items.Add($"[{timestamp}] {message}");
        LogList.ScrollIntoView(LogList.Items[^1]);
    }

    private void ClearLog_Click(object sender, RoutedEventArgs e)
    {
        LogList.Items.Clear();
        DropHint.Visibility = Visibility.Visible;
        UploadProgress.Visibility = Visibility.Collapsed;
        StatusText.Text = "Uploading...";
        ProgressBar.Value = 0;
        ProgressText.Text = "0 / 0 files";
    }

    private void Cancel_Click(object sender, RoutedEventArgs e)
    {
        _cts?.Cancel();
    }
}
