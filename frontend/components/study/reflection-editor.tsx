'use client'

import { useEditor, EditorContent } from '@tiptap/react'
import StarterKit from '@tiptap/starter-kit'
import { useEffect } from 'react'
import { cn } from '@/lib/utils'

interface ReflectionEditorProps {
  content: string
  editable: boolean
  onChange?: (html: string) => void
  className?: string
}

/**
 * 노션처럼 입력창에 마크다운이 즉시 반영되는 WYSIWYG 에디터.
 * StarterKit의 input rules 덕분에 "# ", "- ", "> ", "**굵게**" 등을
 * 입력하는 즉시 해당 스타일로 변환된다.
 */
export function ReflectionEditor({
  content,
  editable,
  onChange,
  className,
}: ReflectionEditorProps) {
  const editor = useEditor({
    extensions: [
      StarterKit.configure({
        heading: { levels: [1, 2, 3] },
      }),
    ],
    content,
    editable,
    immediatelyRender: false,
    editorProps: {
      attributes: {
        class: cn(
          'markdown-body focus:outline-none',
          editable && 'min-h-48',
        ),
      },
    },
    onUpdate: ({ editor }) => {
      onChange?.(editor.getHTML())
    },
  })

  // editable 상태가 바뀌면 에디터에 반영
  useEffect(() => {
    editor?.setEditable(editable)
  }, [editor, editable])

  // 외부에서 content가 교체되면(예: 편집 시작/취소) 동기화
  useEffect(() => {
    if (editor && content !== editor.getHTML()) {
      editor.commands.setContent(content, { emitUpdate: false })
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [content, editor])

  return <EditorContent editor={editor} className={className} />
}
