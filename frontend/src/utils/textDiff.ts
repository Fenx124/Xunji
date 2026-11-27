/** 行级文本差异（非 Myers 精确算法，侧重可读对比） */

export type DiffLineType = 'equal' | 'added' | 'removed'

export interface DiffLine {
  type: DiffLineType
  text: string
}

/**
 * 将历史文本（old）与当前文本（new）按行对比。
 * removed：旧版有、走向新版时删掉；added：新版新增。
 */
export function lineDiff(oldText: string, newText: string): DiffLine[] {
  const oldLines = oldText.split('\n')
  const newLines = newText.split('\n')
  const result: DiffLine[] = []
  let i = 0
  let j = 0

  while (i < oldLines.length || j < newLines.length) {
    const a = oldLines[i]
    const b = newLines[j]

    if (i < oldLines.length && j < newLines.length && a === b) {
      result.push({ type: 'equal', text: a })
      i++
      j++
      continue
    }

    const aInRestOfNew = i < oldLines.length && newLines.slice(j).includes(a)

    if (j < newLines.length && !aInRestOfNew) {
      result.push({ type: 'added', text: b })
      j++
    } else if (i < oldLines.length) {
      result.push({ type: 'removed', text: a })
      i++
    } else if (j < newLines.length) {
      result.push({ type: 'added', text: b })
      j++
    }
  }

  return result
}