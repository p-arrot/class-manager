import { readdir, readFile } from 'node:fs/promises'
import { join, relative } from 'node:path'
import { fileURLToPath } from 'node:url'

const root = fileURLToPath(new URL('..', import.meta.url))
const sourceRoot = join(root, 'src')

const checks = [
  {
    name: 'no-debug-console',
    pattern: /console\.(log|warn|error)\s*\(/,
    message: 'Use user-facing feedback or a shared logger instead of raw console calls.',
  },
  {
    name: 'no-explicit-any',
    pattern: /\bany\b/,
    message: 'Use an explicit domain type or unknown with narrowing instead of any.',
    ignoreLine: line => line.includes('Is any form field focused?'),
  },
  {
    name: 'no-ts-ignore',
    pattern: /@ts-(ignore|expect-error)/,
    message: 'Fix the type boundary instead of suppressing TypeScript.',
  },
  {
    name: 'no-inline-svg',
    pattern: /<svg\b|viewBox=/,
    message: 'Use the existing icon library or a componentized asset instead of inline SVG.',
  },
]

const targetExtensions = new Set(['.ts', '.tsx', '.vue'])

function extensionOf(file) {
  const index = file.lastIndexOf('.')
  return index >= 0 ? file.slice(index) : ''
}

async function listFiles(dir) {
  const entries = await readdir(dir, { withFileTypes: true })
  const files = await Promise.all(entries.map(async entry => {
    const fullPath = join(dir, entry.name)
    if (entry.isDirectory()) return listFiles(fullPath)
    return targetExtensions.has(extensionOf(entry.name)) ? [fullPath] : []
  }))
  return files.flat()
}

const violations = []

for (const file of await listFiles(sourceRoot)) {
  const content = await readFile(file, 'utf8')
  const lines = content.split(/\r?\n/)
  lines.forEach((line, index) => {
    for (const check of checks) {
      if (check.ignoreLine?.(line)) continue
      if (!check.pattern.test(line)) continue
      violations.push({
        check: check.name,
        file: relative(root, file).replaceAll('\\', '/'),
        line: index + 1,
        message: check.message,
      })
    }
  })
}

if (violations.length) {
  console.error('Quality gate failed:')
  for (const violation of violations) {
    console.error(`- [${violation.check}] ${violation.file}:${violation.line} ${violation.message}`)
  }
  process.exit(1)
}

console.log('Quality gate passed.')
