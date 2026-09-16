# Read-only local resource report. Run from the repository root or any directory.
$os = [System.Environment]::OSVersion.VersionString
$cpu = $env:PROCESSOR_IDENTIFIER
$memoryType = @'
using System;
using System.Runtime.InteropServices;
public static class LocalMemoryStatus {
    [StructLayout(LayoutKind.Sequential)]
    public struct MEMORYSTATUSEX {
        public uint Length;
        public uint MemoryLoad;
        public ulong TotalPhysical;
        public ulong AvailablePhysical;
        public ulong TotalPageFile;
        public ulong AvailablePageFile;
        public ulong TotalVirtual;
        public ulong AvailableVirtual;
        public ulong AvailableExtendedVirtual;
    }
    [DllImport("kernel32.dll", SetLastError = true)]
    public static extern bool GlobalMemoryStatusEx(ref MEMORYSTATUSEX status);
}
'@
if (-not ('LocalMemoryStatus' -as [type])) { Add-Type -TypeDefinition $memoryType }
$memory = [LocalMemoryStatus+MEMORYSTATUSEX]::new()
$memory.Length = [System.Runtime.InteropServices.Marshal]::SizeOf($memory)
if (-not [LocalMemoryStatus]::GlobalMemoryStatusEx([ref]$memory)) {
    throw 'Unable to read physical memory status.'
}
$driveName = (Get-Location).Drive.Name
$disk = Get-PSDrive -Name $driveName -PSProvider FileSystem

function First-Line($command, $arguments) {
    if (-not (Get-Command $command -ErrorAction SilentlyContinue)) { return 'unavailable' }
    try {
        $result = & $command @arguments 2>&1 | Select-Object -First 1
        return [string]$result
    } catch { return 'unavailable' }
}

Write-Output "Operating system: $os"
Write-Output "CPU: $cpu"
Write-Output ('Total RAM: {0:N1} GiB' -f ($memory.TotalPhysical / 1GB))
Write-Output ('Available RAM: {0:N1} GiB' -f ($memory.AvailablePhysical / 1GB))
Write-Output ('Free disk space ({0}:): {1:N1} GiB' -f $driveName, ($disk.Free / 1GB))
Write-Output "Java version: $(First-Line 'java' @('-version'))"
Write-Output "Node version: $(First-Line 'node' @('--version'))"
Write-Output "Docker version: $(First-Line 'docker' @('--version'))"
Write-Output "Git version: $(First-Line 'git' @('--version'))"
