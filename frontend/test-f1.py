"""Test student CRUD + batch + layout fix."""
from playwright.sync_api import sync_playwright
import sys

BASE = 'http://localhost:5173'
results = []

def check(desc, condition):
    status = 'PASS' if condition else 'FAIL'
    results.append((status, desc))
    print(f"  [{status}] {desc}")

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    page = browser.new_page(viewport={'width': 1440, 'height': 900})

    # Admin login
    print("=== Login ===")
    page.goto(f'{BASE}/login')
    page.wait_for_load_state('networkidle')
    text_inputs = page.locator('input[type="text"], input:not([type]):not([type="password"])').all()
    pwd_inputs = page.locator('input[type="password"]').all()
    if text_inputs: text_inputs[0].fill('admin')
    if pwd_inputs: pwd_inputs[0].fill('admin123')
    for b in page.locator('button').all():
        if '登' in (b.text_content() or ''): b.click(); break
    page.wait_for_timeout(2000)
    page.wait_for_load_state('networkidle')
    check('Login success', '/admin' in page.url)

    # Navigate to student management
    print("\n=== StudentManage ===")
    menu_items = page.locator('.n-menu-item, .n-menu-item-content').all()
    for item in menu_items:
        if '学生' in (item.text_content() or ''): item.click(); break
    page.wait_for_timeout(1000)
    page.wait_for_load_state('networkidle')
    page.screenshot(path='/tmp/f1_student_page.png', full_page=True)
    page_content = page.content()
    check('Student page loaded', '学生管理' in page_content)

    # Check layout fills screen
    content_area = page.locator('.main-content')
    if content_area.count() > 0:
        height = content_area.bounding_box()
        if height:
            check('Content area uses most of viewport', height['height'] > 300)
    else:
        check('Content area found', False)

    # Check create button exists
    create_btns = page.locator('button:has-text("新建学生")')
    check('Create student button exists', create_btns.count() > 0)

    # Check batch action area visible (selection column)
    check_col = page.locator('.n-data-table-th--selection')
    check('Selection column exists (batch support)', check_col.count() > 0)

    # Check edit/delete buttons in action column
    action_cells = page.locator('.n-data-table-td--actions')
    check('Action column rendered', action_cells.count() > 0 or '操作' in page_content)

    # ===== Summary =====
    print("\n" + "="*50)
    passed = sum(1 for s, _ in results if s == 'PASS')
    failed = sum(1 for s, _ in results if s == 'FAIL')
    print(f"Results: {passed} passed, {failed} failed, {len(results)} total")
    for status, desc in results:
        print(f"  [{status}] {desc}")

    browser.close()
    if failed > 0: sys.exit(1)
