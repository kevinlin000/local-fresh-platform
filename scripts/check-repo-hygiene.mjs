import fs from 'node:fs'
import path from 'node:path'
import process from 'node:process'

const rootDir = process.cwd()
const ignoredDirs = new Set([
  '.git',
  '.idea',
  '.vscode',
  'dist',
  'node_modules',
  'target'
])
const ignoredFiles = new Set([
  'AGENTS.md',
  'CLAUDE.md',
  'package-lock.json',
  'pnpm-lock.yaml',
  'check-repo-hygiene.mjs',
  'yarn.lock'
])
const textExtensions = new Set([
  '.css',
  '.html',
  '.java',
  '.js',
  '.json',
  '.md',
  '.mjs',
  '.properties',
  '.scss',
  '.sh',
  '.sql',
  '.ts',
  '.tsx',
  '.vue',
  '.xml',
  '.yaml',
  '.yml'
])
const forbiddenTerms = [
  '蒼穹外賣',
  'skytakeout',
  'wechat',
  '微信',
  '高德',
  '阿里'
]

const failures = []

function relative(filePath) {
  return path.relative(rootDir, filePath).replaceAll(path.sep, '/')
}

function isTextFile(filePath) {
  const basename = path.basename(filePath)
  if (ignoredFiles.has(basename)) {
    return false
  }
  return textExtensions.has(path.extname(filePath))
}

function walk(dir, files = []) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    if (entry.isDirectory()) {
      if (!ignoredDirs.has(entry.name)) {
        walk(path.join(dir, entry.name), files)
      }
      continue
    }
    if (entry.isFile()) {
      files.push(path.join(dir, entry.name))
    }
  }
  return files
}

function checkScreenshotReferences(readmePath) {
  const content = fs.readFileSync(readmePath, 'utf8')
  const refs = [...content.matchAll(/!\[[^\]]*]\((docs\/screenshots\/[^)\s]+)\)/g)]
    .map((match) => match[1])
  if (refs.length !== 9) {
    failures.push(`${relative(readmePath)} should reference 9 screenshots, found ${refs.length}`)
  }
  for (const ref of refs) {
    const absoluteRef = path.join(rootDir, ref)
    if (!fs.existsSync(absoluteRef)) {
      failures.push(`${relative(readmePath)} references missing screenshot: ${ref}`)
    }
  }
}

function checkTextFile(filePath) {
  const rel = relative(filePath)
  const content = fs.readFileSync(filePath, 'utf8')
  const lines = content.split(/\n/)

  lines.forEach((line, index) => {
    if (/[ \t]$/.test(line)) {
      failures.push(`${rel}:${index + 1} has trailing whitespace`)
    }
  })

  for (const term of forbiddenTerms) {
    const lineIndex = lines.findIndex((line) => line.includes(term))
    if (lineIndex !== -1) {
      failures.push(`${rel}:${lineIndex + 1} contains legacy term "${term}"`)
    }
  }
}

for (const readme of ['README.md', 'README.en.md']) {
  checkScreenshotReferences(path.join(rootDir, readme))
}

for (const filePath of walk(rootDir)) {
  if (isTextFile(filePath)) {
    checkTextFile(filePath)
  }
}

if (failures.length > 0) {
  console.error('Repository hygiene check failed:')
  for (const failure of failures) {
    console.error(`- ${failure}`)
  }
  process.exit(1)
}

console.log('Repository hygiene check passed.')
