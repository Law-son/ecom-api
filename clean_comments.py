import os
import re

def should_keep_comment(comment_text, next_line):
    """Determine if a comment should be kept based on complexity."""
    # Keep comments for complex logic or non-obvious code
    keep_keywords = [
        'CRITICAL', 'IMPORTANT', 'WARNING', 'NOTE', 'TODO', 'FIXME',
        'algorithm', 'performance', 'security', 'thread-safe',
        'workaround', 'hack', 'edge case'
    ]
    
    # Check if comment contains important keywords
    for keyword in keep_keywords:
        if keyword.lower() in comment_text.lower():
            return True
    
    # Remove simple getter/setter docs
    if any(x in next_line for x in ['get', 'set', 'is', 'has']) and len(comment_text) < 100:
        return False
    
    # Remove obvious method descriptions
    obvious_patterns = [
        r'Creates? (a|an|the)',
        r'Returns? (a|an|the)',
        r'Gets? (a|an|the)',
        r'Sets? (a|an|the)',
        r'Deletes? (a|an|the)',
        r'Updates? (a|an|the)',
        r'Retrieves? (a|an|the)',
        r'Lists? (a|an|the)',
        r'Finds? (a|an|the)',
    ]
    
    for pattern in obvious_patterns:
        if re.search(pattern, comment_text, re.IGNORECASE):
            return False
    
    return True

def clean_java_file(filepath):
    """Remove unnecessary JavaDocs and comments from a Java file."""
    with open(filepath, 'r', encoding='utf-8') as f:
        lines = f.readlines()
    
    cleaned_lines = []
    i = 0
    changes_made = False
    
    while i < len(lines):
        line = lines[i]
        
        # Check for JavaDoc comment start
        if line.strip().startswith('/**'):
            # Find the end of JavaDoc
            javadoc_lines = [line]
            i += 1
            while i < len(lines) and '*/' not in lines[i]:
                javadoc_lines.append(lines[i])
                i += 1
            if i < len(lines):
                javadoc_lines.append(lines[i])  # Include closing */
                i += 1
            
            # Get the next non-empty line
            next_line = ''
            j = i
            while j < len(lines) and lines[j].strip() == '':
                j += 1
            if j < len(lines):
                next_line = lines[j]
            
            # Check if we should keep this JavaDoc
            javadoc_text = ''.join(javadoc_lines)
            if should_keep_comment(javadoc_text, next_line):
                cleaned_lines.extend(javadoc_lines)
            else:
                changes_made = True
            continue
        
        # Check for single-line comments
        if line.strip().startswith('//'):
            # Keep if it contains important keywords
            if should_keep_comment(line, ''):
                cleaned_lines.append(line)
            else:
                changes_made = True
            i += 1
            continue
        
        cleaned_lines.append(line)
        i += 1
    
    if changes_made:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.writelines(cleaned_lines)
        return True
    return False

def process_directory(directory):
    """Process all Java files in directory."""
    count = 0
    for root, dirs, files in os.walk(directory):
        for file in files:
            if file.endswith('.java'):
                filepath = os.path.join(root, file)
                if clean_java_file(filepath):
                    count += 1
                    print(f"Cleaned: {filepath}")
    print(f"\nTotal files cleaned: {count}")

if __name__ == '__main__':
    process_directory('src/main/java')
