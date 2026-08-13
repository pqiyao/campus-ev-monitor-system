export type HardwareCamera = {
  id: number
  name: string
  location: string
  videoUrl: string
  detectionUrl: string
  resolution: string
  frameRate: number
}

const HOST = '192.168.1.101'

export const HARDWARE_CAMERAS: ReadonlyArray<HardwareCamera> = Object.freeze([
  { id: 1, name: '局域网摄像头 1', location: '硬件节点 8081', videoUrl: '/hardware-video-1/stream.mjpg', detectionUrl: '/hardware-detection-1/detections', resolution: '640x480', frameRate: 25 },
  { id: 2, name: '局域网摄像头 2', location: '硬件节点 8082', videoUrl: '/hardware-video-2/stream.mjpg', detectionUrl: '/hardware-detection-2/detections', resolution: '640x480', frameRate: 25 },
  { id: 3, name: '局域网摄像头 3', location: '硬件节点 8083', videoUrl: '/hardware-video-3/stream.mjpg', detectionUrl: '/hardware-detection-3/detections', resolution: '640x480', frameRate: 25 }
])

export const HARDWARE_CAMERA_BY_ID = Object.fromEntries(HARDWARE_CAMERAS.map(camera => [camera.id, camera])) as Record<number, HardwareCamera>
