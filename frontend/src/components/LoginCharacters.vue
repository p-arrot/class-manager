<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'

const props = defineProps<{
  emailFocused: boolean
  passwordFocused: boolean
  passwordVisible: boolean
  passwordLength: number
  errorShown: boolean
}>()

// ── Shared mouse ──
const mouseX = ref(0)
const mouseY = ref(0)
function onMouseMove(e: MouseEvent) { mouseX.value = e.clientX; mouseY.value = e.clientY }
onMounted(() => window.addEventListener('mousemove', onMouseMove))
onUnmounted(() => window.removeEventListener('mousemove', onMouseMove))

// ── Time for breathing (rAF) ──
const elapsed = ref(0)
let lastTs = 0
let animFrame = 0
onMounted(() => {
  const loop = (ts: number) => {
    if (lastTs) elapsed.value += (ts - lastTs) / 1000
    lastTs = ts
    animFrame = requestAnimationFrame(loop)
  }
  animFrame = requestAnimationFrame(loop)
})
onUnmounted(() => cancelAnimationFrame(animFrame))

// ── Character personalities ──
interface Persona {
  id: string
  color: string
  width: number; height: number
  borderRadius: string
  xBase: number
  eyeSize: number; eyeGap: number; eyeY: number
  pupilSize: number; pupilMaxDist: number
  zIndex: number; hasMouth: boolean
  skewSensitivity: number
  leanAmount: number; tiptoeAmount: number; privacyLean: number
  reactionDelay: number; peekCuriosity: number
  // Idle breathing
  breatheAmp: number; breathePeriod: number; breathePhase: number
  // Error shock
  shockScale: number; shockRecover: number
}

const characters: Persona[] = [
  // z-order: black(4) front → purple(3) → orange(2) → yellow(1) back
  // xBase close together so they overlap in depth
  { id: 'purple', color: '#7C3AED', width: 80, height: 230, borderRadius: '40px 40px 24px 24px', xBase: 0,   eyeSize: 36, eyeGap: 18, eyeY: 28, pupilSize: 14, pupilMaxDist: 7,   zIndex: 3, hasMouth: false, skewSensitivity: 3.0, leanAmount: 14, tiptoeAmount: 1.10, privacyLean: 7,  reactionDelay: 0,   peekCuriosity: 10, breatheAmp: 0.008, breathePeriod: 2.8, breathePhase: 0,   shockScale: 1.12, shockRecover: 350 },
  { id: 'black',  color: '#1F1F1F', width: 62, height: 136, borderRadius: '31px 31px 16px 16px', xBase: 54,  eyeSize: 28, eyeGap: 12, eyeY: 26, pupilSize: 11, pupilMaxDist: 6,   zIndex: 4, hasMouth: false, skewSensitivity: 2.0, leanAmount: 7,  tiptoeAmount: 1.06, privacyLean: 4,  reactionDelay: 150, peekCuriosity: 6,  breatheAmp: 0.005, breathePeriod: 3.4, breathePhase: 1.2, shockScale: 1.08, shockRecover: 480 },
  { id: 'orange', color: '#F97316', width: 92, height: 160, borderRadius: '46px 46px 22px 22px', xBase: 100, eyeSize: 32, eyeGap: 14, eyeY: 24, pupilSize: 12, pupilMaxDist: 6.5, zIndex: 2, hasMouth: false, skewSensitivity: 2.5, leanAmount: 10, tiptoeAmount: 1.08, privacyLean: 6,  reactionDelay: 80,  peekCuriosity: 8,  breatheAmp: 0.012, breathePeriod: 2.2, breathePhase: 2.8, shockScale: 1.10, shockRecover: 400 },
  { id: 'yellow', color: '#EAB308', width: 54, height: 200, borderRadius: '27px 27px 14px 14px', xBase: 156, eyeSize: 26, eyeGap: 10, eyeY: 24, pupilSize: 10, pupilMaxDist: 5,   zIndex: 1, hasMouth: true,  skewSensitivity: 1.8, leanAmount: 5,  tiptoeAmount: 1.04, privacyLean: 3,  reactionDelay: 250, peekCuriosity: 4,  breatheAmp: 0.006, breathePeriod: 3.8, breathePhase: 4.5, shockScale: 1.05, shockRecover: 580 },
]

// ── Blinking ──
const blinking = ref<Record<string, boolean>>({})
const blinkTimers = new Map<string, ReturnType<typeof setTimeout>>()
function scheduleBlink(id: string) {
  const delay = Math.random() * 4000 + 3000
  const timer = setTimeout(() => {
    blinking.value = { ...blinking.value, [id]: true }
    setTimeout(() => { blinking.value = { ...blinking.value, [id]: false }; scheduleBlink(id) }, 150)
  }, delay)
  blinkTimers.set(id, timer)
}
onMounted(() => characters.forEach(c => scheduleBlink(c.id)))
onUnmounted(() => blinkTimers.forEach(t => clearTimeout(t)))

// ── Body skew from mouse ──
function baseSkew(c: Persona): number {
  const stageCenterX = window.innerWidth * 0.28
  const dx = (mouseX.value - stageCenterX) / stageCenterX
  return Math.max(-4, Math.min(4, dx * c.skewSensitivity))
}

// ── Delayed reaction states ──
const reacting = ref<Record<string, { email: boolean; password: boolean; visible: boolean }>>({})
const reactionTimers = new Map<string, ReturnType<typeof setTimeout>>()
function applyReaction(id: string, email: boolean, password: boolean, visible: boolean) {
  const c = characters.find(x => x.id === id)!
  if (c.reactionDelay === 0) {
    reacting.value = { ...reacting.value, [id]: { email, password, visible } }
  } else {
    clearTimeout(reactionTimers.get(id))
    reactionTimers.set(id, setTimeout(() => {
      reacting.value = { ...reacting.value, [id]: { email, password, visible } }
    }, c.reactionDelay))
  }
}
watch(() => props.emailFocused, (v) => characters.forEach(c => applyReaction(c.id, v, props.passwordFocused, props.passwordVisible)))
watch(() => props.passwordFocused, (v) => characters.forEach(c => applyReaction(c.id, props.emailFocused, v, props.passwordVisible)))
watch(() => props.passwordVisible, (v) => characters.forEach(c => applyReaction(c.id, props.emailFocused, props.passwordFocused, v)))
watch(() => props.passwordLength, () => characters.forEach(c => applyReaction(c.id, props.emailFocused, props.passwordFocused, props.passwordVisible)))
onUnmounted(() => reactionTimers.forEach(t => clearTimeout(t)))

// ── Password-peek cycle ──
const peekBlend = ref(0)  // 0=pretending not to look, 1=fully peeking
let peekTimer: ReturnType<typeof setTimeout> | null = null
let peekAnimFrame: number | null = null
function animatePeekIn() {
  if (peekBlend.value >= 1) return
  peekBlend.value = Math.min(1, peekBlend.value + 0.04)
  peekAnimFrame = requestAnimationFrame(animatePeekIn)
}
function animatePeekOut() {
  if (peekBlend.value <= 0) { peekBlend.value = 0; schedulePeek(); return }
  peekBlend.value = Math.max(0, peekBlend.value - 0.03)
  peekAnimFrame = requestAnimationFrame(animatePeekOut)
}
watch(() => props.passwordVisible && props.passwordLength > 0, (active) => {
  if (active) schedulePeek()
  else { if (peekTimer) clearTimeout(peekTimer); if (peekAnimFrame) cancelAnimationFrame(peekAnimFrame); peekBlend.value = 0 }
})
function schedulePeek() {
  if (!props.passwordVisible || props.passwordLength === 0) return
  peekTimer = setTimeout(() => {
    animatePeekIn()
    setTimeout(() => animatePeekOut(), 600 + Math.random() * 400)
  }, Math.random() * 3000 + 2000)
}
onUnmounted(() => { if (peekTimer) clearTimeout(peekTimer); if (peekAnimFrame) cancelAnimationFrame(peekAnimFrame) })

// ── Error shock ──
const shocked = ref(false)
const shockStart = ref(0) // elapsed when shock began
const SHOCK_DURATION = 0.55 // seconds
let shockTimer: ReturnType<typeof setTimeout> | null = null
watch(() => props.errorShown, (v) => {
  if (v && !shocked.value) {
    shocked.value = true
    shockStart.value = elapsed.value
    shockTimer = setTimeout(() => { shocked.value = false }, SHOCK_DURATION * 1000 + 100)
  }
})
onUnmounted(() => { if (shockTimer) clearTimeout(shockTimer) })

// ── Per-character shock animation (driven by rAF elapsed) ──
function getShockExtra(c: Persona): { scale: number; skewAdd: number; translateX: number } {
  if (!shocked.value) return { scale: 1, skewAdd: 0, translateX: 0 }
  const t = (elapsed.value - shockStart.value) / SHOCK_DURATION // 0..1 normalised
  if (t <= 0 || t >= 1) return { scale: 1, skewAdd: 0, translateX: 0 }
  const s = c.shockScale - 1 // extra scale amount

  if (c.id === 'purple') {
    // Sharp jump up then settle: ease-out
    const jump = s * (1 - t) * (1 - t)
    return { scale: 1 + jump, skewAdd: 0, translateX: 0 }
  }
  if (c.id === 'orange') {
    // Wobble side to side with decaying amplitude
    const decay = 1 - t
    const wobble = Math.sin(t * Math.PI * 6) * 5 * decay
    return { scale: 1 + s * decay, skewAdd: wobble, translateX: wobble * 1.2 }
  }
  if (c.id === 'black') {
    // Freeze first 0.15s, then lean toward purple
    if (t < 0.25) return { scale: 1 + s * 0.5, skewAdd: 0, translateX: 0 }
    const lean = (t - 0.25) / 0.75
    const decay = 1 - lean
    return { scale: 1 + s * 0.2 * decay, skewAdd: -4 * lean * decay, translateX: 6 * lean * decay }
  }
  // yellow: slight flinch, quick recovery
  const flinch = s * (1 - t) * Math.exp(-t * 4)
  return { scale: 1 + flinch, skewAdd: 0, translateX: 0 }
}

// ── Is any form field focused? (pause breathing) ──
const anyFocus = ref(false)
watch(() => props.emailFocused || props.passwordFocused, (v) => { anyFocus.value = v })

// ── Transform per character ──
function getCharStyle(c: Persona) {
  const r = reacting.value[c.id]
  const typingEmail = r?.email ?? false
  const typingPassword = (r?.password ?? false) && props.passwordLength > 0 && !props.passwordVisible
  const pwdVisible = props.passwordVisible && props.passwordLength > 0

  let skew = baseSkew(c)
  let translateX = 0
  let heightScale = 1

  if (pwdVisible) {
    // 计算偷瞄的渐进程度 (peekAmount: 0=假装没看, 1=忍不住偷看)
    const peekAmount = peekBlend.value
    // 站直假装没看 → 身体微前倾靠近 → 忍不住偷看(更前倾)
    const baseLean = -2 - peekAmount * c.peekCuriosity * 0.6
    const baseApproach = 6 + peekAmount * c.peekCuriosity * 1.4
    skew = baseSkew(c) * 0.2 + baseLean
    translateX = baseApproach
    heightScale = 1.01 + peekAmount * 0.02
  } else if (typingEmail) {
    skew = baseSkew(c) - c.leanAmount
    translateX = c.leanAmount * 2.2
    heightScale = c.tiptoeAmount
  } else if (typingPassword) {
    // 输入密码(隐藏)：头果断扭开，身体往远处躲
    skew = baseSkew(c) + c.privacyLean + 3
    translateX = -c.privacyLean * 2.0
    heightScale = c.tiptoeAmount
  }

  // Idle breathing: blend in when nothing is focused
  if (!typingEmail && !typingPassword && !anyFocus.value) {
    const breath = Math.sin(elapsed.value * (2 * Math.PI) / c.breathePeriod + c.breathePhase) * c.breatheAmp
    heightScale += breath
  }

  // Error shock
  const shock = getShockExtra(c)
  heightScale *= shock.scale
  skew += shock.skewAdd
  translateX += shock.translateX

  return {
    transform: `translateX(${c.xBase + translateX}px) skewX(${skew}deg) scaleY(${heightScale})`,
    width: `${c.width}px`,
    height: `${c.height}px`,
    backgroundColor: c.color,
    borderRadius: c.borderRadius,
    zIndex: c.zIndex,
    transition: shocked.value
      ? `transform 0.08s ease-out`
      : `transform 0.38s cubic-bezier(0.25, 0.1, 0.25, 1)`,
  }
}

// ── Pupil tracking ──
const tick = ref(0)
onMounted(() => {
  const l = () => { tick.value++; animFrame = requestAnimationFrame(l) }
  animFrame = requestAnimationFrame(l)
})

function getPupilStyle(id: string, ei: number) {
  void tick.value
  const el = document.getElementById(`char-${id}-eye-${ei}`)
  if (!el) return {}
  const rect = el.getBoundingClientRect()
  const cx = rect.left + rect.width / 2
  const cy = rect.top + rect.height / 2
  const maxD = characters.find(c => c.id === id)?.pupilMaxDist ?? 4
  const angle = Math.atan2(mouseY.value - cy, mouseX.value - cx)
  const dist = Math.min(Math.sqrt((mouseX.value - cx) ** 2 + (mouseY.value - cy) ** 2), maxD)
  return { transform: `translate(${Math.cos(angle) * dist}px, ${Math.sin(angle) * dist}px)` }
}
</script>

<template>
  <div class="characters-stage">
    <div class="ground" />
    <div
      v-for="c in characters"
      :key="c.id"
      class="character"
      :style="getCharStyle(c) as any"
    >
      <div class="eyes-row" :style="{ gap: `${c.eyeGap}px`, top: `${c.eyeY}%` }">
        <div
          v-for="ei in [0, 1]"
          :key="ei"
          :id="`char-${c.id}-eye-${ei}`"
          class="eye"
          :style="{
            width: `${c.eyeSize}px`,
            height: blinking[c.id] ? '2px' : `${c.eyeSize}px`,
            transition: 'all 0.15s ease',
          }"
        >
          <div
            v-if="!blinking[c.id]"
            class="pupil"
            :style="{
              width: `${c.pupilSize}px`,
              height: `${c.pupilSize}px`,
              ...getPupilStyle(c.id, ei),
            }"
          />
        </div>
      </div>
      <div v-if="c.hasMouth" class="mouth" />
    </div>
  </div>
</template>

<style scoped>
.characters-stage {
  position: relative;
  width: 100%;
  height: 420px;
  margin-top: 8px;
}
.ground {
  position: absolute;
  bottom: 10px;
  left: 5%;
  right: 5%;
  height: 1px;
  background: linear-gradient(to right, rgba(0,0,0,0.06), rgba(0,0,0,0.03), transparent);
}
.character {
  position: absolute;
  bottom: 12px;
  left: 10%;
  transform-origin: bottom center;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  will-change: transform;
}
.eyes-row {
  position: absolute;
  display: flex;
  justify-content: center;
}
.eye {
  background: #ffffff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  box-shadow: inset 0 0 0 1px rgba(0,0,0,0.05);
}
.pupil {
  background: #1a1a1a;
  border-radius: 50%;
  transition: transform 0.1s ease-out;
}
.mouth {
  position: absolute;
  top: 46%;
  left: 50%;
  transform: translateX(-50%);
  width: 14px;
  height: 3px;
  border-radius: 2px;
  opacity: 0.2;
  background: currentColor;
}
</style>
