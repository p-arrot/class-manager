import { ref, onUnmounted } from 'vue'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

export function useRealtime() {
  const connected = ref(false)
  const lastUpdate = ref<any>(null)
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

  function subscribeTask(taskId: number, callback: (data: any) => void) {
    if (!client) return
    client.subscribe(`/topic/task/${taskId}`, (msg) => {
      const data = JSON.parse(msg.body)
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
