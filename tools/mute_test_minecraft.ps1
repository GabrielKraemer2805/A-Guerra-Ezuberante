$ErrorActionPreference='Stop'
# Change only Minecraft/Java audio sessions, never the Windows master volume or other apps.
Add-Type -TypeDefinition @'
using System;
using System.Diagnostics;
using System.Runtime.InteropServices;
[ComImport,Guid("BCDE0395-E52F-467C-8E3D-C4579291692E")] class AudioEnumerator {}
[ComImport,Guid("A95664D2-9614-4F35-A746-DE8DB63617E6"),InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
interface IDevices {
 [PreserveSig]int EnumAudioEndpoints(int flow,int mask,out IntPtr devices);
 [PreserveSig]int GetDefaultAudioEndpoint(int flow,int role,out IDevice device);
}
[ComImport,Guid("D666063F-1587-4E43-81F1-B948E807363F"),InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
interface IDevice { [PreserveSig]int Activate(ref Guid iid,int context,IntPtr parameters,[MarshalAs(UnmanagedType.IUnknown)]out object instance); }
[ComImport,Guid("77AA99A0-1BD6-484F-8BC7-2C654C9A9B6F"),InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
interface ISessions {
 [PreserveSig]int GetAudioSessionControl(IntPtr guid,int flags,out IntPtr control);
 [PreserveSig]int GetSimpleAudioVolume(IntPtr guid,int flags,out IntPtr volume);
 [PreserveSig]int GetSessionEnumerator(out ISessionList list);
}
[ComImport,Guid("E2F5BB11-0570-40CA-ACDD-3AA01277DEE8"),InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
interface ISessionList {
 [PreserveSig]int GetCount(out int count);
 [PreserveSig]int GetSession(int index,out ISession session);
}
[ComImport,Guid("bfb7ff88-7239-4fc9-8fa2-07c950be9c6d"),InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
interface ISession {
 [PreserveSig]int GetState(out int state);
 [PreserveSig]int GetDisplayName([MarshalAs(UnmanagedType.LPWStr)]out string name);
 [PreserveSig]int SetDisplayName([MarshalAs(UnmanagedType.LPWStr)]string name,ref Guid context);
 [PreserveSig]int GetIconPath([MarshalAs(UnmanagedType.LPWStr)]out string path);
 [PreserveSig]int SetIconPath([MarshalAs(UnmanagedType.LPWStr)]string path,ref Guid context);
 [PreserveSig]int GetGroupingParam(out Guid grouping);
 [PreserveSig]int SetGroupingParam(ref Guid grouping,ref Guid context);
 [PreserveSig]int RegisterAudioSessionNotification(IntPtr client);
 [PreserveSig]int UnregisterAudioSessionNotification(IntPtr client);
 [PreserveSig]int GetSessionIdentifier([MarshalAs(UnmanagedType.LPWStr)]out string identifier);
 [PreserveSig]int GetSessionInstanceIdentifier([MarshalAs(UnmanagedType.LPWStr)]out string identifier);
 [PreserveSig]int GetProcessId(out uint id);
}
[ComImport,Guid("87CE5498-68D6-44E5-9215-6DA47EF883D8"),InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
interface IVolume {
 [PreserveSig]int SetMasterVolume(float value,ref Guid context);
 [PreserveSig]int GetMasterVolume(out float value);
 [PreserveSig]int SetMute([MarshalAs(UnmanagedType.Bool)]bool mute,ref Guid context);
 [PreserveSig]int GetMute([MarshalAs(UnmanagedType.Bool)]out bool mute);
}
public static class MinecraftTestAudio {
 public static int Mute(int[] processIds) {
  var devices=(IDevices)new AudioEnumerator();IDevice device;
  Marshal.ThrowExceptionForHR(devices.GetDefaultAudioEndpoint(0,1,out device));
  var iid=typeof(ISessions).GUID;object raw;
  Marshal.ThrowExceptionForHR(device.Activate(ref iid,23,IntPtr.Zero,out raw));
  var sessions=(ISessions)raw;ISessionList list;sessions.GetSessionEnumerator(out list);
  int count;list.GetCount(out count);int changed=0;var context=Guid.Empty;
  for(int i=0;i<count;i++) {
   ISession session;list.GetSession(i,out session);uint pid;session.GetProcessId(out pid);
   if(Array.IndexOf(processIds,(int)pid)<0)continue;
   var volume=(IVolume)session;Marshal.ThrowExceptionForHR(volume.SetMute(true,ref context));changed++;
  }
  return changed;
 }
}
'@
$javaIds=@(Get-CimInstance Win32_Process | Where-Object { $_.Name -match '^javaw?\.exe$' -and $_.CommandLine -match 'run-renan-client|run-renan-observer|run-arsenal-client|run-arsenal-observer|run-pistol-client|run-summon-client|run-summon-observer' } | ForEach-Object { [int]$_.ProcessId })
$count=[MinecraftTestAudio]::Mute([int[]]$javaIds)
Write-Host "Muted $count audio sessions belonging to isolated Minecraft test processes."
