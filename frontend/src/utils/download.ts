export function downloadMarkdown(filename: string, content: string): void {
  const safeName = filename.trim() || '未命名笔记'
  const blob = new Blob([content], { type: 'text/markdown;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = safeName.endsWith('.md') ? safeName : `${safeName}.md`
  anchor.click()
  URL.revokeObjectURL(url)
}