import { ref, onUnmounted } from 'vue'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

export type RealtimePayload = Record<string, unknown>

export function useRealtime() {
  const connected = ref(false)
  const lastUpdate = ref<RealtimePayload | null>(null)
  let client: Client | null = null

  function connect() {
    client = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      reconnectDelay: 5000,
      onConnect: () => { connected.value = true },
      onDisconnect: () => { connected.value = false },
    })
    client.activate()
  }

  function subscribeTask(taskId: number, callback: (data: RealtimePayload) => void) {
    if (!client) return
    client.subscribe(`/topic/task/${taskId}`, (msg) => {
      const parsed = JSON.parse(msg.body) as unknown
      const data = parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed as RealtimePayload : { value: parsed }
      lastUpdate.value = data
      callback(data)
    })
  }

  function disconnect() {
    client?.deactivate()
    connected.value = false
  }

  onUnmounted(disconnect)

  return { connected, lastUpdate, connect, subscribeTask, disconnect }
}
