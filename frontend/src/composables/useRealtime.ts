import { ref, onUnmounted } from 'vue'
import { Client, type StompSubscription } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

export type RealtimePayload = Record<string, unknown>

export function useRealtime() {
  const connected = ref(false)
  const lastUpdate = ref<RealtimePayload | null>(null)
  let client: Client | null = null
  const subscriptions = new Map<string, {
    callback: (data: RealtimePayload) => void
    subscription: StompSubscription | null
  }>()

  function parsePayload(body: string) {
    const parsed = JSON.parse(body) as unknown
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed as RealtimePayload : { value: parsed }
  }

  function attachSubscription(destination: string) {
    const entry = subscriptions.get(destination)
    if (!entry || !client?.connected) return
    entry.subscription?.unsubscribe()
    entry.subscription = client.subscribe(destination, (msg) => {
      const data = parsePayload(msg.body)
      lastUpdate.value = data
      entry.callback(data)
    })
  }

  function connect() {
    if (client?.active) return
    client = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      reconnectDelay: 5000,
      onConnect: () => {
        connected.value = true
        subscriptions.forEach((_entry, destination) => attachSubscription(destination))
      },
      onDisconnect: () => { connected.value = false },
      onWebSocketClose: () => { connected.value = false },
    })
    client.activate()
  }

  function subscribeTask(taskId: number, callback: (data: RealtimePayload) => void) {
    const destination = `/topic/task/${taskId}`
    subscriptions.set(destination, { callback, subscription: null })
    attachSubscription(destination)
  }

  function disconnect() {
    subscriptions.forEach(entry => entry.subscription?.unsubscribe())
    subscriptions.clear()
    client?.deactivate()
    connected.value = false
  }

  onUnmounted(disconnect)

  return { connected, lastUpdate, connect, subscribeTask, disconnect }
}
