from PIL import Image, ImageDraw, ImageFont

OUT = r"c:\Projects\Vibecoding_Personal\Millionaire\.tmp_icon"
font = ImageFont.truetype(r"C:\Windows\Fonts\arial.ttf", 400)
img = Image.new("RGB", (500, 620), (255, 255, 255))
d = ImageDraw.Draw(img)
d.text((250, 310), "\u20b4", font=font, fill=(0, 0, 0), anchor="mm")
# grid every 25 px with labels
for x in range(0, 501, 25):
    d.line([(x, 0), (x, 620)], fill=(210, 210, 210), width=1)
    d.text((x + 2, 2), str(x), fill=(150, 0, 0))
for y in range(0, 621, 25):
    d.line([(0, y), (500, y)], fill=(210, 210, 210), width=1)
    d.text((2, y + 2), str(y), fill=(0, 0, 150))
img.save(OUT + r"\hryvnia_grid.png")

# tight bbox of the black glyph
bbox = img.convert("L").point(lambda p: 0 if p < 128 else 255).getbbox()
print("glyph bbox in grid image:", bbox)
